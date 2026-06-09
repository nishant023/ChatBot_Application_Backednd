package com.Springboot.Chatbot.dto;

public record MessageDto(
        String role,
        String content,
        String timestamp
) {
}
