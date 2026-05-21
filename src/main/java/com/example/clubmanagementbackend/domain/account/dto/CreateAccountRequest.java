package com.example.clubmanagementbackend.domain.account.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateAccountRequest {

    // Backend tự tạo memberId, không nhận từ frontend
    private String memberId;

    // Thông tin thành viên mới
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;

    // Thông tin tài khoản
    private String username;
    private String password;
    private String role;
    private String status;

    // Unit assignment — dùng cho role manager (bắt buộc nếu là manager role)
    // Hoặc để gán membership thông thường
    private String managedUnitId;    // unit chính quản lý (cho group_leader/dept_leader v.v.)
    private String groupUnitId;      // nhóm muốn tham gia (cho member thường)
    private String departmentUnitId; // ban muốn tham gia (cho member thường)
}
