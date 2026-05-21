package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceParticipantResponse {
    private String memberId;
    private String memberName;
    private String fullName;
    private String email;
    private String phone;
    private String unitName;

    /** Trạng thái đăng ký hoạt động (ActivityRegistration.status) */
    private String registrationStatus;

    /** Trạng thái tham gia chương trình (ProgramParticipation.status) */
    private String participationStatus;

    /** Đã được quản lý bấm điểm danh chưa */
    private boolean attended;

    /** Thời điểm điểm danh, null nếu chưa điểm danh */
    private LocalDateTime checkedInAt;

    /** Điểm đã được cộng, 0 nếu chưa điểm danh */
    private int pointsAwarded;
}
