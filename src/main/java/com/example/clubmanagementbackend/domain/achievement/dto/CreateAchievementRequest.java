package com.example.clubmanagementbackend.domain.achievement.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateAchievementRequest {
    private Long participationId;
    private String programId;
    private String memberId;
    private String title;
    private String type;
    private String achievementName;
    private String description;
    private LocalDate achievementDate;
    private String certificateUrl;
    private String status;
}
