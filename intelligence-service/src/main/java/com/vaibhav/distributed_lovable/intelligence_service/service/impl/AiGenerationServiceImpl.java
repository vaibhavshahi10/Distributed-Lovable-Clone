package com.vaibhav.distributed_lovable.intelligence_service.service.impl;

import com.vaibhav.distributed_lovable.common_lib.enums.ChatEventStatus;
import com.vaibhav.distributed_lovable.common_lib.enums.ChatEventType;
import com.vaibhav.distributed_lovable.common_lib.enums.MessageRole;
import com.vaibhav.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.vaibhav.distributed_lovable.common_lib.security.AuthUtil;
import com.vaibhav.distributed_lovable.intelligence_service.client.WorkspaceClient;
import com.vaibhav.distributed_lovable.intelligence_service.dto.chat.StreamResponse;
import com.vaibhav.distributed_lovable.intelligence_service.entity.ChatEvent;
import com.vaibhav.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.vaibhav.distributed_lovable.intelligence_service.entity.ChatSession;
import com.vaibhav.distributed_lovable.intelligence_service.entity.ChatSessionId;
import com.vaibhav.distributed_lovable.intelligence_service.llm.CodeGenerationTools;
import com.vaibhav.distributed_lovable.intelligence_service.llm.FileTreeContextAdvisor;
import com.vaibhav.distributed_lovable.intelligence_service.llm.LlmResponseParser;
import com.vaibhav.distributed_lovable.intelligence_service.llm.PromptUtils;
import com.vaibhav.distributed_lovable.intelligence_service.repository.ChatEventRepository;
import com.vaibhav.distributed_lovable.intelligence_service.repository.ChatMessageRepository;
import com.vaibhav.distributed_lovable.intelligence_service.repository.ChatSessionRepository;
import com.vaibhav.distributed_lovable.intelligence_service.service.AiGenerationService;
import com.vaibhav.distributed_lovable.intelligence_service.service.UsageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AiGenerationServiceImpl implements AiGenerationService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;

    ChatClient chatClient;
    AuthUtil authUtil;
    FileTreeContextAdvisor fileTreeContextAdvisor;
    LlmResponseParser llmResponseParser;
    ChatEventRepository chatEventRepository;
    UsageService usageService;
    WorkspaceClient workspaceClient;
    KafkaTemplate<String, Object> kafkaTemplate;

    private static final Pattern FILE_TAG_PATTERN = Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>", Pattern.DOTALL);

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {

//        usageService.checkDailyTokensUsage();

        Long userId = authUtil.getCurrentUserId();

        ChatSession chatSession = createChatSessionIfNotExists(projectId, userId);

        Map<String, Object> advisorParams = Map.of(
                "userId", userId,
                "projectId", projectId
        );

        StringBuilder fullResponseBuffer = new StringBuilder();

        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectId, workspaceClient);

        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(advisorSpec -> {
                    advisorSpec.params(advisorParams);
                    advisorSpec.advisors(fileTreeContextAdvisor);
                        }
                )
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    if(response.getResults() != null && !response.getResults().isEmpty()){
                        String content = response.getResult().getOutput().getText();

                        if(content != null && !content.isEmpty() && endTime.get() == 0){ // first non-empty chunk received
                            endTime.set(System.currentTimeMillis());
                        }
                        if(response.getMetadata().getUsage() != null){
                            usageRef.set(response.getMetadata().getUsage());
                        }

                        fullResponseBuffer.append(content);
                    }
                })
                .doOnComplete(() -> {
                    Schedulers.boundedElastic().schedule(() -> {
                        long duration = (endTime.get() - startTime.get()) / 1000;
                        finalizeChats(userMessage, chatSession, fullResponseBuffer.toString(), duration, usageRef.get());
                    });
                })
                .doOnError(error -> log.error("Error during streaming for project : {}", projectId))
                .map(response -> {
                    if(response.getResults() != null && !response.getResults().isEmpty()){
                        String text = response.getResult().getOutput().getText();
                        return new StreamResponse(text != null ? text : "");
                    }
                    return new StreamResponse("");
                });
    }

    private void finalizeChats(String userMessage, ChatSession chatSession, String fullText, Long duration, Usage usage){
        Long projectId = chatSession.getId().getProjectId();

        if(usage != null){
            int totalTokens = usage.getTotalTokens();
            usageService.recordTokenUsage(chatSession.getId().getUserId(), totalTokens);
        }

        // save the user message
        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .role(MessageRole.USER)
                        .content(userMessage)
                        .tokensUsed(usage.getPromptTokens())
                        .build()
        );

        ChatMessage assistantChatMessage = ChatMessage.builder()
                .role(MessageRole.ASSISTANT)
                .content("Assistant Message here..")
                .chatSession(chatSession)
                .tokensUsed(usage.getCompletionTokens())
                .build();

        assistantChatMessage = chatMessageRepository.save(assistantChatMessage);

        List<ChatEvent> chatEventList = llmResponseParser.parseChatEvents(fullText, assistantChatMessage);
        chatEventList.addFirst(
                ChatEvent.builder()
                        .type(ChatEventType.THOUGHT)
                        .status(ChatEventStatus.CONFIRMED)
                        .chatMessage(assistantChatMessage)
                        .content("Thought for " + duration + "s")
                        .sequenceOrder(0)
                        .build()
        );

        chatEventList.stream()
                .filter(e -> e.getType() == ChatEventType.FILE_EDIT)
                .forEach(e -> {
                    String sagaId = UUID.randomUUID().toString();
                    e.setSagaId(sagaId);
                    FileStoreRequestEvent fileStoreRequestEvent = new FileStoreRequestEvent(
                            projectId,
                            sagaId,
                            e.getFilePath(),
                            e.getContent(),
                            chatSession.getId().getUserId()
                    );
                    kafkaTemplate.send("file-storage-request-event", "project-" + projectId, fileStoreRequestEvent);
                    log.info("Storage request event sent: {}", e.getFilePath());
                });

        chatEventRepository.saveAll(chatEventList);
    }

    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId){
        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);

        if(chatSession == null){
            chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .build();

            chatSession = chatSessionRepository.save(chatSession);
        }
        return chatSession;
    }
}
