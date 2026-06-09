package com.Springboot.Chatbot.dto;

public record SessionSummary(
        String sessionId,
        String title,
        String updatedAt) {
}
