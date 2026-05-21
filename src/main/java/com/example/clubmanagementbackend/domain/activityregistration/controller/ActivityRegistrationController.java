package com.example.clubmanagementbackend.domain.activityregistration.controller;

import com.example.clubmanagementbackend.domain.activityregistration.dto.ActivityRegistrationResponse;
import com.example.clubmanagementbackend.domain.activityregistration.dto.CreateActivityRegistrationRequest;
import com.example.clubmanagementbackend.domain.activityregistration.dto.UpdateRegistrationStatusRequest;
import com.example.clubmanagementbackend.domain.activityregistration.service.ActivityRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity-registrations")
@RequiredArgsConstructor
public class ActivityRegistrationController {

    private final ActivityRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<ActivityRegistrationResponse> createRegistration(@RequestBody CreateActivityRegistrationRequest request) {
        return ResponseEntity.ok(registrationService.createRegistration(request));
    }

    @GetMapping
    public ResponseEntity<List<ActivityRegistrationResponse>> getRegistrations(
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String activityId,
            @RequestParam(required = false) String managerUserId,
            @RequestParam(required = false) String unitId) {
        return ResponseEntity.ok(registrationService.getRegistrations(memberId, activityId, managerUserId, unitId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ActivityRegistrationResponse> updateRegistrationStatus(
            @PathVariable Long id,
            @RequestBody UpdateRegistrationStatusRequest request) {
        return ResponseEntity.ok(registrationService.updateStatus(id, request));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<ActivityRegistrationResponse>> getRegistrationsByMember(@PathVariable String memberId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByMember(memberId));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ActivityRegistrationResponse> cancelRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.cancelRegistration(id));
    }
}
