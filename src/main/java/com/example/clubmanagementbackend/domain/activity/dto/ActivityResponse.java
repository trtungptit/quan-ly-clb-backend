package com.example.clubmanagementbackend.domain.activity.dto;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
@Data @Builder
public class ActivityResponse {
    private String activityId;
    private String activityName;
    private String name; // maps to activityName for frontend compatibility
    private String unitId;
    private String unitName;
    private String unitType;
    private String groupId;
    private String departmentId;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registerDeadline;
    private LocalDate registrationDeadline; // maps to registerDeadline for frontend compatibility
    private LocalDate cancelDeadline;
    private LocalDate cancellationDeadline; // maps to cancelDeadline for frontend compatibility
    private String location;
    private boolean isLimited;
    private Integer maxParticipants;
    private String createdBy;
    private String managerUserId; // maps to createdBy for frontend compatibility
    private String status;
    private LocalDate createdDate;
}

