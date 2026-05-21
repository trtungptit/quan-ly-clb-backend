package com.example.clubmanagementbackend.domain.annualprogram.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class AnnualProgramResponse {
    private String programId;
    private String programName;
    private String name; // maps name for frontend
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registerDeadline;
    private LocalDate registrationDeadline; // maps registerDeadline for frontend
    private LocalDate cancelDeadline;
    private String location;
    private Integer maxParticipants;
    private Integer year;
    private String status;
}
