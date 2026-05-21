package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CheckInResponse {
    private boolean success;
    private String message;
    private int pointsAwarded;
    private int totalMonthlyPoints;
}
