package com.example.clubmanagementbackend.domain.programparticipation.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProgramParticipationResponse {
    private Long participationId;
    private String programId;
    private String programName;
    private String memberId;
    private String memberName;
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private String unitId;
    private String unitName;
    private String unitType;
    private String note;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime createdAt; // alias for createdDate
}
