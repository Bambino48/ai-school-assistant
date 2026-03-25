package com.ibdev.assistant.controller;

import com.ibdev.assistant.dto.ChatRequest;
import com.ibdev.assistant.dto.MessageResponse;
import com.ibdev.assistant.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final AiService aiService;

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/api/chat/test-json")
    public MessageResponse testJson() {
        return new MessageResponse("Backend OK", "success");
    }

    @PostMapping("/api/chat")
    public MessageResponse chat(@RequestBody ChatRequest request) {

        String userMessage = request.getMessage();

        if (userMessage == null || userMessage.isEmpty()) {
            return new MessageResponse("Message vide interdit", "error");
        }

        String aiResponse = aiService.askAi(userMessage);

        return new MessageResponse(aiResponse, "success");
    }
}
