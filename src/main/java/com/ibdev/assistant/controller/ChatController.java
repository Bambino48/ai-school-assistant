package com.ibdev.assistant.controller;

import com.ibdev.assistant.dto.ChatRequest;
import com.ibdev.assistant.dto.ConversationDto;
import com.ibdev.assistant.dto.MessageDto;
import com.ibdev.assistant.dto.MessageResponse;
import com.ibdev.assistant.entity.Conversation;
import com.ibdev.assistant.entity.Message;
import com.ibdev.assistant.repository.ConversationRepository;
import com.ibdev.assistant.repository.MessageRepository;
import com.ibdev.assistant.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final AiService aiService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatController(AiService aiService,
                          ConversationRepository conversationRepository,
                          MessageRepository messageRepository) {
        this.aiService = aiService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @PostMapping("/chat")
    public MessageResponse chat(@RequestBody ChatRequest request) {
        try {
            String userMessage = request.getMessage();

            if (userMessage == null || userMessage.isEmpty()) {
                return new MessageResponse("Message vide interdit", "error");
            }

            Conversation conversation = new Conversation();
            conversation = conversationRepository.save(conversation);

            Message userMsg = new Message();
            userMsg.setContent(userMessage);
            userMsg.setRole("USER");
            userMsg.setConversation(conversation);
            messageRepository.save(userMsg);

            String aiResponse = aiService.askAi(userMessage);

            Message aiMsg = new Message();
            aiMsg.setContent(aiResponse);
            aiMsg.setRole("AI");
            aiMsg.setConversation(conversation);
            messageRepository.save(aiMsg);

            return new MessageResponse(aiResponse, "success");

        } catch (Exception e) {
            e.printStackTrace();
            return new MessageResponse("Erreur serveur interne", "error");
        }
    }

    @GetMapping("/conversations")
    public List<ConversationDto> getConversations() {
        return conversationRepository.findAll()
                .stream()
                .map(conversation -> new ConversationDto(
                        conversation.getId(),
                        conversation.getCreatedAt()
                ))
                .toList();
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageDto> getMessagesByConversation(@PathVariable Long id) {
        return messageRepository.findByConversationIdOrderByTimestampAsc(id)
                .stream()
                .map(message -> new MessageDto(
                        message.getContent(),
                        message.getRole()
                ))
                .toList();
    }
}