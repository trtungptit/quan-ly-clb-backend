package com.example.clubmanagementbackend.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long notificationId;
    private String memberId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String readStatus;
    private boolean isRead;
    private boolean read; // alias for isRead
    private String type;
    private String relatedId;
}
