package com.example.clubmanagementbackend.domain.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_activity_points",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "year", "month"})
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyActivityPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", length = 50, nullable = false)
    private String memberId;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Builder.Default
    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
