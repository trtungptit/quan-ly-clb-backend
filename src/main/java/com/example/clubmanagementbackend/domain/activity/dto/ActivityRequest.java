package com.example.clubmanagementbackend.domain.activity.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ActivityRequest {
    // Frontend gửi "name", backend cũ dùng "activityName" — hỗ trợ cả hai
    private String name;
    private String activityName;
    private String unitId;
    private String groupId;
    private String departmentId;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registerDeadline;
    private LocalDate cancelDeadline;
    private String location;
    private boolean isLimited;
    private Integer maxParticipants;
    private String status;          // frontend gửi status string
    private String managerUserId;   // to identify the user making the request
    private String createdBy;        // frontend cũ có thể gửi createdBy thay cho managerUserId

    /** Trả về tên hoạt động: ưu tiên field "name" (frontend), fallback "activityName" */
    public String getEffectiveName() {
        if (name != null && !name.isBlank()) return name;
        return activityName;
    }

    /** Trả về unitId: ưu tiên unitId, fallback groupId/departmentId từ frontend cũ */
    public String getEffectiveUnitId() {
        if (unitId != null && !unitId.isBlank()) return unitId;
        if (groupId != null && !groupId.isBlank()) return groupId;
        if (departmentId != null && !departmentId.isBlank()) return departmentId;
        return null;
    }

    /** Trả về người tạo/quản lý: ưu tiên managerUserId, fallback createdBy */
    public String getEffectiveManagerUserId() {
        if (managerUserId != null && !managerUserId.isBlank()) return managerUserId;
        return createdBy;
    }
}
