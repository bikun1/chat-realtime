package com.example.chat_realtime.repository;

import com.example.chat_realtime.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}