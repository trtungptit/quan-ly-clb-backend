package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualAttendanceRequest {
    /** userId của quản lý/chủ tịch thực hiện điểm danh */
    private String checkedByUserId;
}
