package com.example.clubmanagementbackend.domain.annualprogram.entity;

import com.example.clubmanagementbackend.common.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "annual_programs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnualProgram {
    @Id @Column(length = 50) private String programId;
    @Column(nullable = false) private String programName;
    @Column(length = 1000) private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registerDeadline;
    private LocalDate cancelDeadline;
    private String location;
    private Integer maxParticipants;
    private Integer year;
    @Enumerated(EnumType.STRING) private Status status;
    private boolean deleted;
}
