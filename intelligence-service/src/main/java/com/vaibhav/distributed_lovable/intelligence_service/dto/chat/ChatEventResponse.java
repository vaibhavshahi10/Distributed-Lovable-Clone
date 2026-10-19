package com.vaibhav.distributed_lovable.intelligence_service.dto.chat;


import com.vaibhav.distributed_lovable.common_lib.enums.ChatEventType;

public record ChatEventResponse(
        Long id,
        ChatEventType type,
        Integer sequenceOrder,
        String content,
        String filePath,
        String metadata
) {
}
