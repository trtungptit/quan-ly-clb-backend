package com.example.clubmanagementbackend.domain.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long messageId;
    private String unitId;
    private String unitName;
    private String unitType; // "group" / "department"
    private String senderMemberId;
    private String senderUserId;
    private String senderName;
    private String senderRole;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;
}
