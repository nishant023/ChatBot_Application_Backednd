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
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatClient chatClient;



    //how many recent messages to always keep in full
    private static final int RECENT_WINDOW = 20;

    //summarize when total messages cross this threshold
    private static final int SUMMARIZE_THRESHOLD = 20;

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
    public ChatResponse sendMessage(String sessionId, String userId, String message) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Save user message
        ChatMessage userMsg = new ChatMessage("user", message, Instant.now());
        session.getMessages().add(userMsg);

        // Auto-set title from first message
        if (session.getMessages().size() == 1) {
            String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
            session.setTitle(title);
        }

        // ✅ Check if summarization needed before building prompt
        handleSummarization(session);

        String historyPrompt = buildHistoryPrompt(session, message);

        String aiResponse = chatClient.prompt()
                .user(historyPrompt)
                .call()
                .content();

        ChatMessage assistantMsg = new ChatMessage("assistant", aiResponse, Instant.now());
        session.getMessages().add(assistantMsg);
        session.setUpdatedAt(Instant.now());
        chatSessionRepository.save(session);

        return new ChatResponse(aiResponse, sessionId);
    }
    /*
    @Override
    public ChatResponse sendMessage(String sessionId, String userId, String message) {
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
     */


    @Override
    public Flux<String> streamMessage(String sessionId, String userId, String message) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Save user message
        ChatMessage userMsg = new ChatMessage("user", message, Instant.now());
        session.getMessages().add(userMsg);

        // Auto-set title from first message
        if (session.getMessages().size() == 1) {
            String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
            session.setTitle(title);
        }

        // ✅ Check if summarization needed before building prompt
        handleSummarization(session);

        String historyPrompt = buildHistoryPrompt(session, message);

        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .user(historyPrompt)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    String completeReply = fullResponse.toString();
                    ChatMessage assistantMsg = new ChatMessage("assistant", completeReply, Instant.now());
                    session.getMessages().add(assistantMsg);
                    session.setUpdatedAt(Instant.now());
                    chatSessionRepository.save(session);
                })
                .doOnError(error ->
                        System.err.println("Streaming error for session " + sessionId + ": " + error.getMessage())
                );
    }

    /*
    @Override
    public Flux<String> streamMessage(String sessionId, String userId, String message) {

        // 1. Load and validate session
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // 2. Save user message
        ChatMessage userMsg = new ChatMessage("user", message, Instant.now());
        session.getMessages().add(userMsg);

        // 3. Auto-set title from first message
        if (session.getMessages().size() == 1) {
            String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
            session.setTitle(title);
        }

        // 4. Build full history prompt
        String historyPrompt = buildHistoryPrompt(session);

        // 5. StringBuilder to collect full AI response as chunks arrive
        StringBuilder fullResponse = new StringBuilder();

        // 6. Stream from Gemini — tap each chunk, collect it, then save to DB on complete
        return chatClient.prompt()
                .user(historyPrompt)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    // Each chunk arrives here — collect it for DB save later
                    fullResponse.append(chunk);
                })
                .doOnComplete(() -> {
                    // All chunks received — now save full assistant reply to MongoDB
                    String completeReply = fullResponse.toString();
                    ChatMessage assistantMsg = new ChatMessage("assistant", completeReply, Instant.now());
                    session.getMessages().add(assistantMsg);
                    session.setUpdatedAt(Instant.now());
                    chatSessionRepository.save(session);
                })
                .doOnError(error -> {
                    // Optional: log streaming errors
                    System.err.println("Streaming error for session " + sessionId + ": " + error.getMessage());
                });
    }
     */

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


    private String buildHistoryPrompt(ChatSession session) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : session.getMessages()) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        sb.append("assistant:");
        return sb.toString();
    }


    /**
     * Checks if the session has crossed the summarization threshold.
     * If yes — takes all messages OLDER than the recent window,
     * summarizes them via Gemini, saves summary back to session,
     * and removes those old messages from the list.
     */
    private void handleSummarization(ChatSession session) {
        int totalMessages = session.getMessages().size();

        // Not enough messages yet — nothing to summarize
        if (totalMessages <= SUMMARIZE_THRESHOLD) {
            return;
        }

        // Messages to summarize = everything except the last RECENT_WINDOW messages
        int cutoff = totalMessages - RECENT_WINDOW;
        List<ChatMessage> oldMessages = session.getMessages().subList(0, cutoff);

        // Build a prompt asking Gemini to summarize the old messages
        StringBuilder toSummarize = new StringBuilder();
        toSummarize.append("Summarize the following conversation in 3-5 sentences, ");
        toSummarize.append("capturing the key topics, decisions, and important details discussed. ");
        toSummarize.append("This summary will be used as context for continuing the conversation:\n\n");

        for (ChatMessage msg : oldMessages) {
            toSummarize.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        // If there's an existing summary, include it so context is never lost
        if (session.getConversationSummary() != null && !session.getConversationSummary().isBlank()) {
            toSummarize.insert(0,
                    "Previous summary: " + session.getConversationSummary() + "\n\n" +
                            "New messages to add to the summary:\n\n"
            );
        }

        // Call Gemini to produce the summary
        String newSummary = chatClient.prompt()
                .user(toSummarize.toString())
                .call()
                .content();

        // Save the new summary to the session
        session.setConversationSummary(newSummary);

        // Remove the old messages that were just summarized — keep only recent window
        List<ChatMessage> recentMessages = new ArrayList<>(
                session.getMessages().subList(cutoff, totalMessages)
        );
        session.setMessages(recentMessages);
    }

    /**
     * Builds the prompt sent to Gemini.
     * If a summary exists — prepends it so Gemini has full context.
     * Always includes the last RECENT_WINDOW messages verbatim.
     */
    private String buildHistoryPrompt(ChatSession session, String currentMessage) {
        StringBuilder prompt = new StringBuilder();

        // Prepend summary if it exists
        if (session.getConversationSummary() != null && !session.getConversationSummary().isBlank()) {
            prompt.append("Context summary of earlier conversation:\n");
            prompt.append(session.getConversationSummary());
            prompt.append("\n\n---\n\nRecent conversation:\n");
        }

        // Add recent messages (already trimmed to RECENT_WINDOW by handleSummarization)
        for (ChatMessage msg : session.getMessages()) {
            prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        prompt.append("assistant:");
        return prompt.toString();
    }


}
