package com.Springboot.Chatbot.service.Impl;

import com.Springboot.Chatbot.dto.*;
import com.Springboot.Chatbot.entity.ChatMessage;
import com.Springboot.Chatbot.entity.ChatSession;
import com.Springboot.Chatbot.repository.ChatSessionRepository;
import com.Springboot.Chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatClient chatClient;

    @Override
    public CreateSessionResponse createSession(String userId) {
        ChatSession chatSession = new ChatSession();
        System.out.println("new chat session is started and user id is:" + userId);
        chatSession.setUserId(userId);
        chatSession.setTitle("New Chat");

        chatSession.setCreatedAt(Instant.now());
        chatSession.setUpdatedAt(Instant.now());

        ChatSession savedChatSession = chatSessionRepository.save(chatSession);
        System.out.println("Saved session ID: " + savedChatSession.getId());
        System.out.println("Saved session userId: " + savedChatSession.getUserId());
        System.out.println("Method is about to end");
        return new CreateSessionResponse(
                savedChatSession.getId(),
                savedChatSession.getTitle(),
                savedChatSession.getCreatedAt().toString());
    }

    @Override
    public ChatResponse     sendMessage(String sessionId, String userId, String message) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // Ownership check
        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Save user message
        session.getMessages().add(new ChatMessage("user", message, Instant.now()));

        // Set title from first message
        if (session.getMessages().size() == 1) {
            String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
            session.setTitle(title);
        }

        // Build conversation history prompt
        StringBuilder historyPrompt = new StringBuilder();
        for (ChatMessage msg : session.getMessages()) {
            historyPrompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        historyPrompt.append("assistant:");

        // Call Gemini
        String aiResponse = chatClient.prompt()
                .user(historyPrompt.toString())
                .call()
                .content();

        // Save assistant message
        session.getMessages().add(new ChatMessage("assistant", aiResponse, Instant.now()));
        session.setUpdatedAt(Instant.now());

        chatSessionRepository.save(session);

        return new ChatResponse(aiResponse, sessionId);
    }

    @Override
    public List<SessionSummary> getUserSessions(String userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(s -> new SessionSummary(s.getId(), s.getTitle(), s.getUpdatedAt().toString()))
                .toList();
    }

    @Override
    public SessionDetailResponse getSession(String sessionId, String userId) {

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        List<MessageDto> messages = session.getMessages().stream()
                .map(m -> new MessageDto(m.getRole(), m.getContent(), m.getTimestamp().toString()))
                .toList();

        return new SessionDetailResponse(
                session.getId(),
                session.getTitle(),
                messages,
                session.getCreatedAt().toString()
        );

    }
}
