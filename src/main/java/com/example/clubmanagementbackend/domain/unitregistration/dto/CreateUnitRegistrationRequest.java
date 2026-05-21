package com.example.clubmanagementbackend.domain.unitregistration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUnitRegistrationRequest {
    private String memberId;
    private String unitId;
    private String note;
}
