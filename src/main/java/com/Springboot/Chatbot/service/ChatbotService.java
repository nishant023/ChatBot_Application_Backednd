package com.Springboot.Chatbot.service;

import com.Springboot.Chatbot.dto.ChatbotResponse;

public interface ChatbotService {
    ChatbotResponse chat(String message);
}
