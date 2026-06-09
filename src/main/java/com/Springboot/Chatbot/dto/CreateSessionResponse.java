package com.Springboot.Chatbot.dto;

public record CreateSessionResponse(
        String sessionId,
        String title,
        String createdAt
) {
}
