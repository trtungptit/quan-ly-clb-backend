package com.example.clubmanagementbackend.domain.programparticipation.dto;

import lombok.Data;

@Data
public class CreateProgramParticipationRequest {
    private String programId;
    private String memberId;
    private String note;
}
