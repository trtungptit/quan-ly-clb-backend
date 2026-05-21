package com.example.clubmanagementbackend.domain.group.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder
public class ClubUnitResponse {
    private String unitId;
    private String unitName;
    private String type;
    private String description;
    private String status;
}
