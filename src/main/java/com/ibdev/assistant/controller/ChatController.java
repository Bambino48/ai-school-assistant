package com.ibdev.assistant.controller;

import com.ibdev.assistant.dto.ChatRequest;
import com.ibdev.assistant.dto.ChatStreamSession;
import com.ibdev.assistant.dto.ConversationDto;
import com.ibdev.assistant.dto.MessageDto;
import com.ibdev.assistant.dto.MessageResponse;
import com.ibdev.assistant.service.ConversationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ConversationService conversationService;

    public ChatController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/chat")
    public MessageResponse chat(@RequestBody ChatRequest request) {
        return conversationService.processChat(
                request.getMessage(),
                request.getConversationId()
        );
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamChat(@RequestBody ChatRequest request) {
        ChatStreamSession session = conversationService.processChatStream(
                request.getMessage(),
                request.getConversationId()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("X-Conversation-Id", String.valueOf(session.conversationId()))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(session.stream());
    }

    @GetMapping("/conversations")
    public List<ConversationDto> getConversations() {
        return conversationService.getAllConversations();
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageDto> getMessagesByConversation(@PathVariable Long id) {
        return conversationService.getMessagesByConversation(id);
    }

    @DeleteMapping("/conversations/{id}")
    public void deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
    }
}