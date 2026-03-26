package com.ibdev.assistant.controller;

import com.ibdev.assistant.dto.ChatRequest;
import com.ibdev.assistant.dto.ConversationDto;
import com.ibdev.assistant.dto.MessageDto;
import com.ibdev.assistant.dto.MessageResponse;
import com.ibdev.assistant.service.ConversationService;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/conversations")
    public List<ConversationDto> getConversations() {
        return conversationService.getAllConversations();
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageDto> getMessagesByConversation(@PathVariable Long id) {
        return conversationService.getMessagesByConversation(id);
    }
}