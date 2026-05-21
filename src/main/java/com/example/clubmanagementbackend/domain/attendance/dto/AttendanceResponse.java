package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private boolean success;
    private String message;
    private String memberId;
    private String activityId;
    private String programId;
    private int pointsAwarded;
    private int totalMonthlyPoints;
    private LocalDateTime checkedInAt;
}
