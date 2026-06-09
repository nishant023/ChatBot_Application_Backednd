package com.Springboot.Chatbot.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "chats")
public class ChatSession {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;    //The First user message will be the title for the chat

    private List<ChatMessage> messages = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
