package com.example.clubmanagementbackend.domain.programparticipation.entity;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.domain.annualprogram.entity.AnnualProgram;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "program_participations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProgramParticipation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private AnnualProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private String note;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status;

    private LocalDateTime createdDate;
}
