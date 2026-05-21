package com.example.clubmanagementbackend.domain.unitregistration.controller;

import com.example.clubmanagementbackend.domain.unitregistration.dto.CreateUnitRegistrationRequest;
import com.example.clubmanagementbackend.domain.unitregistration.dto.UnitRegistrationResponse;
import com.example.clubmanagementbackend.domain.unitregistration.dto.UpdateRegistrationStatusRequest;
import com.example.clubmanagementbackend.domain.unitregistration.service.UnitRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unit-registrations")
@RequiredArgsConstructor
public class UnitRegistrationController {

    private final UnitRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<UnitRegistrationResponse> createRegistration(@RequestBody CreateUnitRegistrationRequest request) {
        return ResponseEntity.ok(registrationService.createRegistration(request));
    }

    @GetMapping
    public ResponseEntity<List<UnitRegistrationResponse>> getRegistrations(
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String unitId,
            @RequestParam(required = false) String managerUserId) {
        return ResponseEntity.ok(registrationService.getRegistrations(memberId, unitId, managerUserId));
    }

    /**
     * Lấy toàn bộ đơn đăng ký nhóm/ban của một member.
     * Frontend dùng để kiểm tra đơn pending → disable nút gửi lại.
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<UnitRegistrationResponse>> getRegistrationsByMember(
            @PathVariable String memberId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByMember(memberId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UnitRegistrationResponse> updateRegistrationStatus(
            @PathVariable Long id,
            @RequestBody UpdateRegistrationStatusRequest request) {
        return ResponseEntity.ok(registrationService.updateStatus(id, request));
    }
}

