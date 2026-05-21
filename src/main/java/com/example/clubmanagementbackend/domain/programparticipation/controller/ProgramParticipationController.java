package com.example.clubmanagementbackend.domain.programparticipation.controller;

import com.example.clubmanagementbackend.domain.programparticipation.dto.CreateProgramParticipationRequest;
import com.example.clubmanagementbackend.domain.programparticipation.dto.ProgramParticipationResponse;
import com.example.clubmanagementbackend.domain.programparticipation.dto.UpdateParticipationStatusRequest;
import com.example.clubmanagementbackend.domain.programparticipation.service.ProgramParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/program-participations")
@RequiredArgsConstructor
public class ProgramParticipationController {
    private final ProgramParticipationService participationService;

    @GetMapping
    public ResponseEntity<List<ProgramParticipationResponse>> getAll(
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String programId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(participationService.getParticipations(memberId, programId, status));
    }

    @PostMapping
    public ResponseEntity<ProgramParticipationResponse> createParticipation(@RequestBody CreateProgramParticipationRequest request) {
        return ResponseEntity.ok(participationService.createParticipation(request));
    }

    @PutMapping("/{participationId}/status")
    public ResponseEntity<ProgramParticipationResponse> updateStatus(
            @PathVariable Long participationId,
            @RequestBody UpdateParticipationStatusRequest request) {
        return ResponseEntity.ok(participationService.updateStatus(participationId, request));
    }

    @PutMapping("/{participationId}/cancel")
    public ResponseEntity<ProgramParticipationResponse> cancelParticipation(@PathVariable Long participationId) {
        return ResponseEntity.ok(participationService.cancelParticipation(participationId));
    }
}
