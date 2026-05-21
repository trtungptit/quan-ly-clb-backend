package com.example.clubmanagementbackend.domain.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "check_in_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    // ACTIVITY or PROGRAM
    @Column(name = "target_type", length = 20, nullable = false)
    private String targetType;

    @Column(name = "activity_id", length = 50)
    private String activityId;

    @Column(name = "program_id", length = 50)
    private String programId;

    @Column(name = "token", unique = true, nullable = false)
    private String token;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_by_user_id", length = 50)
    private String createdByUserId;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
