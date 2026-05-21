package com.example.clubmanagementbackend.domain.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LoginResponse {
    private String userId;
    private String memberId;
    private String username;
    private String role;
    private String status;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
}
