package com.example.clubmanagementbackend.domain.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "activity_id"}),
        @UniqueConstraint(columnNames = {"member_id", "program_id"})
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @Column(name = "member_id", length = 50, nullable = false)
    private String memberId;

    @Column(name = "activity_id", length = 50)
    private String activityId;

    @Column(name = "program_id", length = 50)
    private String programId;

    @Column(name = "registration_id")
    private Long registrationId;

    @Column(name = "participation_id")
    private Long participationId;

    // ACTIVITY or PROGRAM
    @Column(name = "attendance_type", length = 20, nullable = false)
    private String attendanceType;

    @Column(name = "unit_id", length = 50)
    private String unitId;

    @Column(name = "points_awarded", nullable = false)
    private int pointsAwarded;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    /** userId của manager/chủ tịch thực hiện điểm danh */
    @Column(name = "checked_by_user_id", length = 50)
    private String checkedByUserId;

    // CHECKED_IN
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "CHECKED_IN";

    // MANUAL (thay thế QR)
    @Builder.Default
    @Column(name = "source", length = 20)
    private String source = "MANUAL";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
