package com.example.clubmanagementbackend.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_id", length = 50, nullable = false)
    private String unitId;

    @Column(name = "sender_member_id", length = 50, nullable = false)
    private String senderMemberId;

    @Column(name = "sender_user_id", length = 50)
    private String senderUserId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_role")
    private String senderRole;

    @Column(name = "content", length = 2000, nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "message_type", length = 50, nullable = false)
    private String messageType = "TEXT";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
