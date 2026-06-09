package com.Springboot.Chatbot.dto;

import java.util.List;

public record SessionDetailResponse(
        String sessionId,
        String title,
        List<MessageDto> messages,
        String createAt
) {
}
