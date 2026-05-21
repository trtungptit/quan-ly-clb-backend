package com.example.clubmanagementbackend.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    private String title;
    private String content;
    private String memberId;
    private List<String> memberIds;

    // Backward compatibility for older frontend code.
    private List<String> receiverIds;
}
