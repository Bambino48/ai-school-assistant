package com.ibdev.assistant.repository;

import com.ibdev.assistant.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}