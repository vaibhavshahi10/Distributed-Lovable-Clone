package com.vaibhav.distributed_lovable.intelligence_service.service.impl;

import com.vaibhav.distributed_lovable.common_lib.security.AuthUtil;
import com.vaibhav.distributed_lovable.intelligence_service.dto.chat.ChatResponse;
import com.vaibhav.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.vaibhav.distributed_lovable.intelligence_service.entity.ChatSession;
import com.vaibhav.distributed_lovable.intelligence_service.entity.ChatSessionId;
import com.vaibhav.distributed_lovable.intelligence_service.mapper.ChatMapper;
import com.vaibhav.distributed_lovable.intelligence_service.repository.ChatMessageRepository;
import com.vaibhav.distributed_lovable.intelligence_service.repository.ChatSessionRepository;
import com.vaibhav.distributed_lovable.intelligence_service.service.ChatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class ChatServiceImpl implements ChatService {

    ChatMessageRepository chatMessageRepository;
    ChatSessionRepository chatSessionRepository;
    ChatMapper chatMapper;
    AuthUtil authUtil;

    @Override
    public List<ChatResponse> getProjectChatHistory(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

        ChatSession chatSession = chatSessionRepository.getReferenceById(
                new ChatSessionId(projectId, userId)
        );

        List<ChatMessage> chatMessageList = chatMessageRepository.findByChatSession(chatSession);

        return chatMapper.toListOfChatResponse(chatMessageList);
    }
}
