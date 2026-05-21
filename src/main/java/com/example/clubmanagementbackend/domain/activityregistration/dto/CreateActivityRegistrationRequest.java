package com.example.clubmanagementbackend.domain.activityregistration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityRegistrationRequest {
    private String memberId;
    private String activityId;
    private String note;
}
