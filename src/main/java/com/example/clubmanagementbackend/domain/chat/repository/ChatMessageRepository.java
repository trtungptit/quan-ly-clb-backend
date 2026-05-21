package com.example.clubmanagementbackend.domain.chat.repository;

import com.example.clubmanagementbackend.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUnitIdOrderByCreatedAtAsc(String unitId);
    Optional<ChatMessage> findFirstByUnitIdOrderByCreatedAtDesc(String unitId);
}
