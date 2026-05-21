package com.example.clubmanagementbackend.domain.group.controller;
import com.example.clubmanagementbackend.domain.group.dto.AssignMemberRequest;
import com.example.clubmanagementbackend.domain.group.dto.ClubUnitResponse;
import com.example.clubmanagementbackend.domain.group.dto.MemberUnitResponse;
import com.example.clubmanagementbackend.domain.group.dto.UpdateMemberUnitRequest;
import com.example.clubmanagementbackend.domain.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    // ─── Unit endpoints (/api/units) ─────────────────────────────────────────

    @GetMapping("/api/units")
    public ResponseEntity<List<ClubUnitResponse>> getAllUnits() {
        return ResponseEntity.ok(groupService.getAllUnits());
    }
    @GetMapping("/api/units/groups")
    public ResponseEntity<List<ClubUnitResponse>> getGroups() {
        return ResponseEntity.ok(groupService.getGroups());
    }
    @GetMapping("/api/units/departments")
    public ResponseEntity<List<ClubUnitResponse>> getDepartments() {
        return ResponseEntity.ok(groupService.getDepartments());
    }
    @PostMapping("/api/units/{unitId}/assign")
    public ResponseEntity<MemberUnitResponse> assignMember(
            @PathVariable String unitId,
            @RequestBody AssignMemberRequest request) {
        return ResponseEntity.ok(groupService.assignMember(unitId, request));
    }

    // ─── MemberUnit endpoints (/api/member-units) ────────────────────────────

    /**
     * Cập nhật position hoặc status của một member trong unit.
     * Chỉ manager của unit đó mới được phép thao tác.
     *
     * PATCH /api/member-units/{memberUnitId}?managerUserId=...
     * Body: { "position": "deputy", "status": "active" }
     */
    @PatchMapping("/api/member-units/{memberUnitId}")
    public ResponseEntity<MemberUnitResponse> updateMemberUnit(
            @PathVariable String memberUnitId,
            @RequestParam String managerUserId,
            @RequestBody UpdateMemberUnitRequest request) {
        return ResponseEntity.ok(groupService.updateMemberUnit(memberUnitId, managerUserId, request));
    }
}

