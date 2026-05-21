package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyPointResponse {
    private String memberId;
    private String memberName;
    private String fullName;
    private String email;
    private String unitName;
    private int year;
    private int month;
    private int totalPoints;
    /** "OK" nếu totalPoints >= 10, "LOW" nếu < 10 */
    private String status;
}
