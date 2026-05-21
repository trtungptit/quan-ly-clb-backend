package com.example.clubmanagementbackend.domain.unitregistration.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitRegistrationResponse {
    private Long id;
    private String memberId;
    private String memberName;
    private String unitId;
    private String unitName;
    private String unitType;
    private String note;
    private String status;
    private LocalDateTime createdAt;
}
