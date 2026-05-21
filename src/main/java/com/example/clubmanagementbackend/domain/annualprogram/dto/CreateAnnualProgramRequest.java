package com.example.clubmanagementbackend.domain.annualprogram.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateAnnualProgramRequest {
    private String programName;
    private String name; // mapped from frontend
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registerDeadline;
    private LocalDate cancelDeadline;
    private String location;
    private Integer maxParticipants;
    private Integer year;
    private String status;
}
