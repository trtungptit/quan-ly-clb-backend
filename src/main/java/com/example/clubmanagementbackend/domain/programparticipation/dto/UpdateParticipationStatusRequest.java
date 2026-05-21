package com.example.clubmanagementbackend.domain.programparticipation.dto;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import lombok.Data;

@Data
public class UpdateParticipationStatusRequest {
    private String status;
}
