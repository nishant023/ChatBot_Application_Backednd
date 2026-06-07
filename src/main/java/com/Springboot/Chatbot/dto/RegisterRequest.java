package com.Springboot.Chatbot.dto;

public record RegisterRequest(
        String name,
        String email,
        String password
) {
}
