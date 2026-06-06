package com.Springboot.Chatbot.controller;

import com.Springboot.Chatbot.dto.ChatbotRequest;
import com.Springboot.Chatbot.dto.ChatbotResponse;
import com.Springboot.Chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping
    public ChatbotResponse chat(@RequestBody ChatbotRequest request) {
        return chatbotService.chat(request.message());
    }
}
