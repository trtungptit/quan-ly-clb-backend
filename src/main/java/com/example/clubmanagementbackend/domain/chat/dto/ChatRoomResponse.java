package com.example.clubmanagementbackend.domain.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private String unitId;
    private String unitName;
    private String unitType; // "group" / "department"
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
}
