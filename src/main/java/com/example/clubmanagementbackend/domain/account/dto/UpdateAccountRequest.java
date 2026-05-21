package com.example.clubmanagementbackend.domain.account.dto;

import lombok.Data;

@Data
public class UpdateAccountRequest {
    private String username;
    private String password;

    // Dùng String thay vì enum để tránh Jackson parse lỗi với lowercase
    private String role;
    private String status;

    private String managedUnitId;
}
