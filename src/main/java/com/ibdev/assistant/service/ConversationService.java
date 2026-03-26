package com.ibdev.assistant.service;

import com.ibdev.assistant.dto.ChatStreamSession;
import com.ibdev.assistant.dto.ConversationDto;
import com.ibdev.assistant.dto.MessageDto;
import com.ibdev.assistant.dto.MessageResponse;
import com.ibdev.assistant.entity.Conversation;
import com.ibdev.assistant.entity.Message;
import com.ibdev.assistant.repository.ConversationRepository;
import com.ibdev.assistant.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AiService aiService;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               AiService aiService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiService = aiService;
    }

    public MessageResponse processChat(String userMessage, Long conversationId) {
        try {
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return new MessageResponse("Message vide interdit", "error");
            }

            Conversation conversation;
            List<Message> historyBeforeReply;

            if (conversationId != null) {
                conversation = conversationRepository.findById(conversationId)
                        .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

                historyBeforeReply = messageRepository
                        .findByConversationIdOrderByTimestampAsc(conversation.getId());
            } else {
                conversation = new Conversation();
                conversation = conversationRepository.save(conversation);
                historyBeforeReply = List.of();
            }

            Message userMsg = new Message();
            userMsg.setContent(userMessage);
            userMsg.setRole("USER");
            userMsg.setConversation(conversation);
            messageRepository.save(userMsg);

            String aiResponse = aiService.askAiWithContext(historyBeforeReply, userMessage);

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

    public ChatStreamSession processChatStream(String userMessage, Long conversationId) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new RuntimeException("Message vide interdit");
        }

        Conversation conversation;
        List<Message> historyBeforeReply;

        if (conversationId != null) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation introuvable"));

            historyBeforeReply = messageRepository
                    .findByConversationIdOrderByTimestampAsc(conversation.getId());
        } else {
            conversation = new Conversation();
            conversation = conversationRepository.save(conversation);
            historyBeforeReply = List.of();
        }

        Message userMsg = new Message();
        userMsg.setContent(userMessage);
        userMsg.setRole("USER");
        userMsg.setConversation(conversation);
        messageRepository.save(userMsg);

        final Conversation finalConversation = conversation;
        Long finalConversationId = conversation.getId();

        StreamingResponseBody stream = aiService.streamAiWithContext(
                historyBeforeReply,
                userMessage,
                aiResponse -> {
                    Message aiMsg = new Message();
                    aiMsg.setContent(aiResponse);
                    aiMsg.setRole("AI");
                    aiMsg.setConversation(finalConversation);
                    messageRepository.save(aiMsg);
                }
        );

        return new ChatStreamSession(finalConversationId, stream);
    }

    public List<ConversationDto> getAllConversations() {
        return conversationRepository.findAllByOrderByIdDesc()
                .stream()
                .map(conversation -> new ConversationDto(
                        conversation.getId(),
                        conversation.getCreatedAt()
                ))
                .toList();
    }

    public List<MessageDto> getMessagesByConversation(Long conversationId) {
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId)
                .stream()
                .map(message -> new MessageDto(
                        message.getContent(),
                        message.getRole()
                ))
                .toList();
    }

    public void deleteConversation(Long id) {
        conversationRepository.deleteById(id);
    }
}