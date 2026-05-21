package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CheckInRequest {
    private String memberId;
    private String userId;
}
