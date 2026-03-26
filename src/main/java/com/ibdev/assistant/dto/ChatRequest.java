package com.ibdev.assistant.dto;

public class ChatRequest {

    private String message;
    private Long conversationId;

    public String getMessage() {
        return message;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
}