package com.example.clubmanagementbackend.domain.achievement.entity;

import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.annualprogram.entity.AnnualProgram;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.programparticipation.entity.ProgramParticipation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Achievement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long achievementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = true)
    private ProgramParticipation participation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = true)
    private AnnualProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @Column(nullable = false)
    private String achievementName;

    @Column(length = 1000)
    private String description;

    private LocalDate achievementDate;

    private String certificateUrl;

    private String type;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime createdDate;
}

