package com.example.clubmanagementbackend.domain.chat.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendChatMessageRequest {
    private String unitId;
    private String senderMemberId;
    private String senderUserId;
    private String content;
}
