package com.example.clubmanagementbackend.domain.group.dto;
import lombok.Data;
@Data
public class AssignMemberRequest {
    private String memberId;
    private String position; // leader, deputy, member
}
