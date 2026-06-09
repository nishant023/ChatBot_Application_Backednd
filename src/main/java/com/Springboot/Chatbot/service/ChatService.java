package com.Springboot.Chatbot.service;

import com.Springboot.Chatbot.dto.ChatResponse;
import com.Springboot.Chatbot.dto.CreateSessionResponse;
import com.Springboot.Chatbot.dto.SessionDetailResponse;
import com.Springboot.Chatbot.dto.SessionSummary;

import java.util.List;

public interface ChatService {
    CreateSessionResponse createSession(String userId);

    ChatResponse sendMessage(String sessionId, String userId, String message);

    List<SessionSummary> getUserSessions(String userId);

    SessionDetailResponse getSession(String sessionId, String userId);
}
