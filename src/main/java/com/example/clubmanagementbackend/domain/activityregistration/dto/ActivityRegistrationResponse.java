package com.example.clubmanagementbackend.domain.activityregistration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRegistrationResponse {
    private Long id;
    private String registrationId; // string representation of id
    private String memberId;
    private String activityId;
    private String activityName;
    private String unitId;
    private String unitName;
    private String unitType;
    private String memberName;
    private String fullName;
    private String email;
    private String phone;
    private String note;
    private String status;
    private LocalDateTime createdAt;
}
