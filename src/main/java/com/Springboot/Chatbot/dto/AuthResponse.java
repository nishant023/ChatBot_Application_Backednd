package com.Springboot.Chatbot.dto;

public record AuthResponse(
        String token,
        String name,
        String email
) {
}
