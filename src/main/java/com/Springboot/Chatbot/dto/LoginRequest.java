package com.Springboot.Chatbot.dto;

public record LoginRequest(
        String email,
        String password
) {
}
