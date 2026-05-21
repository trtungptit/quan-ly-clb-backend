package com.example.clubmanagementbackend.domain.group.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Response cho GET /api/members/managed — bao gồm thông tin member_unit
 * để frontend biết memberUnitId và có thể gọi PATCH /api/member-units/{id}
 */
@Data
@Builder
public class ManagedMemberResponse {
    // Thông tin member
    private String memberId;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String status;          // member status lowercase

    // Thông tin member_unit
    private String memberUnitId;    // id trong bảng member_units (UUID String)
    private String unitId;
    private String unitName;
    private String unitType;        // "group" / "department" lowercase
    private String position;        // "member" / "deputy" / "leader" lowercase
    private String memberUnitStatus; // "active" / "inactive" lowercase
    private String accountStatus;   // user_accounts.status lowercase (null nếu không có account)

}
