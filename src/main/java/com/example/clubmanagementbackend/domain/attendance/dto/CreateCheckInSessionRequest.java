package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCheckInSessionRequest {
    private String createdByUserId;
    private LocalDate checkInDate;
    private LocalDateTime expiresAt;
}
