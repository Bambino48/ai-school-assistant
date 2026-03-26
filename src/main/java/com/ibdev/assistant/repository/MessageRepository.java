package com.ibdev.assistant.repository;

import com.ibdev.assistant.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}