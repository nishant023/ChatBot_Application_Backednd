package com.Springboot.Chatbot.repository;

import com.Springboot.Chatbot.entity.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);
}
