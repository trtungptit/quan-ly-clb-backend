package com.example.clubmanagementbackend.domain.group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body cho PATCH /api/member-units/{memberUnitId}?managerUserId=...
 * Frontend gửi lowercase, service tự parse sang enum.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberUnitRequest {
    private String position; // "member", "deputy" (lowercase)
    private String status;   // "active", "inactive" (lowercase)
}
