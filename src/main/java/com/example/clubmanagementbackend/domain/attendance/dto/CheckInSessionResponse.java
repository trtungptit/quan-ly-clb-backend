package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CheckInSessionResponse {
    private Long sessionId;
    private String token;
    private String checkInUrl;
    private String targetType;
    private String activityId;
    private String programId;
    private String title;
    private LocalDate checkInDate;
    private LocalDateTime expiresAt;
    private boolean active;
}
