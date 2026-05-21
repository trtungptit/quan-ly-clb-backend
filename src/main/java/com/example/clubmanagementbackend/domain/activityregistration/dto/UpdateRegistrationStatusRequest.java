package com.example.clubmanagementbackend.domain.activityregistration.dto;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRegistrationStatusRequest {
    private String status;
}
