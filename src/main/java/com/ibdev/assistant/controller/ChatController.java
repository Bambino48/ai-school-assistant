package com.ibdev.assistant.controller;

import com.ibdev.assistant.dto.ChatRequest;
import com.ibdev.assistant.dto.MessageResponse;
import com.ibdev.assistant.entity.Conversation;
import com.ibdev.assistant.entity.Message;
import com.ibdev.assistant.repository.ConversationRepository;
import com.ibdev.assistant.repository.MessageRepository;
import com.ibdev.assistant.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final AiService aiService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatController(AiService aiService, ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.aiService = aiService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
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

        // 1. créer conversation
        Conversation conversation = new Conversation();
        conversation = conversationRepository.save(conversation);

        // 2. message utilisateur
        Message userMsg = new Message();
        userMsg.setContent(userMessage);
        userMsg.setRole("USER");
        userMsg.setConversation(conversation);
        messageRepository.save(userMsg);

        // 3. appel IA
        String aiResponse = aiService.askAi(userMessage);

        // 4. message IA
        Message aiMsg = new Message();
        aiMsg.setContent(aiResponse);
        aiMsg.setRole("AI");
        aiMsg.setConversation(conversation);
        messageRepository.save(aiMsg);

        return new MessageResponse(aiResponse, "success");
    }
}
