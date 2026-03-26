package com.ibdev.assistant.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String role; // USER ou AI

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }

    public void setContent(String content) { this.content = content; }
    public void setRole(String role) { this.role = role; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
}