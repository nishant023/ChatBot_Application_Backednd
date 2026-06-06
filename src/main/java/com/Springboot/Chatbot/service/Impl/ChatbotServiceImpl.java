package com.Springboot.Chatbot.service.Impl;

import com.Springboot.Chatbot.dto.ChatbotResponse;
import com.Springboot.Chatbot.service.ChatbotService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {
    private final ChatClient chatClient;

    // Constructor injection
    public ChatbotServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public ChatbotResponse chat(String message) {
        String response = chatClient.prompt()
                .system("""
                    You are a helpful assistant. Format your responses in a clean, 
                    readable way using:
                    - Clear paragraphs
                    - Bullet points where needed
                    - Simple and easy language
                    - Proper headings if required
                    Do not use unnecessary technical jargon.
                    """)
                .user(message)
                .call()
                .content();

        return new ChatbotResponse(response);
    }

}
