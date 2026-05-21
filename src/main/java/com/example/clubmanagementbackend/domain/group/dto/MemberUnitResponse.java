package com.example.clubmanagementbackend.domain.group.dto;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MemberUnitResponse {
    private String memberUnitId;   // renamed from 'id' for clarity
    private String memberId;
    private String memberName;     // full name of member
    private String unitId;
    private String unitName;       // name of the club unit
    private String unitType;       // lowercase: "group" / "department"
    private String position;       // lowercase
    private String status;         // member_unit status lowercase
    private String accountStatus;  // user_accounts.status lowercase (null nếu không có account)
}


