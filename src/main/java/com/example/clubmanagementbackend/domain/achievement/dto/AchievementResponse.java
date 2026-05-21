package com.example.clubmanagementbackend.domain.achievement.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AchievementResponse {
    private Long achievementId;
    private Long participationId;
    private String programId;
    private String memberId;
    private String memberName;
    private String fullName;
    private String programName;
    private String title; // matches frontend title
    private String achievementName;
    private String description;
    private LocalDate achievementDate;
    private String certificateUrl;
    private String type;
    private String status;
    private LocalDateTime createdDate;
}
