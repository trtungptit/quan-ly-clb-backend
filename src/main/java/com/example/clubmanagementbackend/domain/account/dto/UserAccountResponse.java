package com.example.clubmanagementbackend.domain.account.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserAccountResponse {
    private String userId;
    private String memberId;
    private String username;
    private String role;
    private String status;
    private String managedUnitId;
    // Thông tin thành viên liên kết
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
}
