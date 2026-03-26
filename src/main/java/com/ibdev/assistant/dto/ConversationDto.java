package com.ibdev.assistant.dto;

import java.time.LocalDateTime;

public class ConversationDto {

    private Long id;
    private LocalDateTime createdAt;

    public ConversationDto(Long id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}