package com.Springboot.Chatbot.controller;

import com.Springboot.Chatbot.dto.*;
import com.Springboot.Chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    //creating  session started
    @PostMapping("/session")
    public ResponseEntity<CreateSessionResponse> createSession(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.createSession(userDetails.getUsername()));
    }

    //send message in session
    @PostMapping("/{sessionId}")
    public ResponseEntity<ChatResponse> sendMessage(
            @PathVariable String sessionId,
            @RequestBody ChatRequest chatRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.sendMessage(sessionId, userDetails.getUsername(), chatRequest.message()));
    }

    //all session of logged-in user
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionSummary>> getSessions(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.getUserSessions(userDetails.getUsername()));
    }

    //fetching full session with message
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionDetailResponse> getSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(chatService.getSession(sessionId, userDetails.getUsername()));
    }


    @PostMapping(
            value = "/{sessionId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE   // tells Spring to stream SSE
    )
    public Flux<ServerSentEvent<String>> streamMessage(
            @PathVariable String sessionId,
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        return chatService.streamMessage(sessionId, userDetails.getUsername(), request.message())
                .map(chunk ->
                        ServerSentEvent.<String>builder()
                                .event("message")       // event type the frontend listens for
                                .data(chunk)            // the actual text chunk
                                .build()
                )
                // Send a final [DONE] event so the frontend knows streaming is complete
                .concatWith(
                        Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("done")
                                        .data("[DONE]")
                                        .build()
                        )
                );
    }
}
