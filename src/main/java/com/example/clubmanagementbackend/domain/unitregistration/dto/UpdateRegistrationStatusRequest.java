package com.example.clubmanagementbackend.domain.unitregistration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRegistrationStatusRequest {
    private String status;
}
