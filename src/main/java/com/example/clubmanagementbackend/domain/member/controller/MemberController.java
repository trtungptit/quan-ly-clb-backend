package com.example.clubmanagementbackend.domain.member.controller;

import com.example.clubmanagementbackend.domain.group.dto.ManagedMemberResponse;
import com.example.clubmanagementbackend.domain.group.dto.MemberUnitResponse;
import com.example.clubmanagementbackend.domain.member.dto.MemberResponse;
import com.example.clubmanagementbackend.domain.member.dto.UpdateMemberRequest;
import com.example.clubmanagementbackend.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public List<MemberResponse> getAllMembers() {
        return memberService.getAllMembers();
    }

    /**
     * Trả danh sách member thuộc unit do manager quản lý.
     * Response bao gồm memberUnitId để frontend gọi PATCH /api/member-units/{id}.
     */
    @GetMapping("/managed")
    public ResponseEntity<List<ManagedMemberResponse>> getManagedMembers(
            @RequestParam String managerUserId) {
        return ResponseEntity.ok(memberService.getManagedMembers(managerUserId));
    }

    @GetMapping("/{memberId}")
    public MemberResponse getMemberById(@PathVariable String memberId) {
        return memberService.getMemberById(memberId);
    }


    /**
     * Trả danh sách nhóm/ban thật mà member đang thuộc về.
     * Frontend dùng endpoint này để chỉ hiển thị hoạt động đúng nhóm/ban của thành viên.
     */
    @GetMapping("/{memberId}/units")
    public ResponseEntity<List<MemberUnitResponse>> getMemberUnits(@PathVariable String memberId) {
        return ResponseEntity.ok(memberService.getMemberUnits(memberId));
    }

    @PatchMapping("/{memberId}")
    public MemberResponse updateMember(
            @PathVariable String memberId,
            @RequestBody UpdateMemberRequest request
    ) {
        return memberService.updateMember(memberId, request);
    }
}


