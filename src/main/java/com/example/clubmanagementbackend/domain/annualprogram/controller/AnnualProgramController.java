package com.example.clubmanagementbackend.domain.annualprogram.controller;

import com.example.clubmanagementbackend.domain.annualprogram.dto.AnnualProgramResponse;
import com.example.clubmanagementbackend.domain.annualprogram.dto.CreateAnnualProgramRequest;
import com.example.clubmanagementbackend.domain.annualprogram.service.AnnualProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/annual-programs")
@RequiredArgsConstructor
public class AnnualProgramController {
    private final AnnualProgramService programService;

    @GetMapping
    public ResponseEntity<List<AnnualProgramResponse>> getAll() {
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    @GetMapping("/completed")
    public ResponseEntity<List<AnnualProgramResponse>> getCompleted() {
        return ResponseEntity.ok(programService.getCompletedPrograms());
    }

    @PostMapping
    public ResponseEntity<AnnualProgramResponse> createProgram(@RequestBody CreateAnnualProgramRequest request) {
        return ResponseEntity.ok(programService.createProgram(request));
    }

    @PatchMapping("/{programId}")
    public ResponseEntity<AnnualProgramResponse> updateProgram(
            @PathVariable String programId,
            @RequestBody CreateAnnualProgramRequest request) {
        return ResponseEntity.ok(programService.updateProgram(programId, request));
    }

    @DeleteMapping("/{programId}")
    public ResponseEntity<Void> deleteProgram(@PathVariable String programId) {
        programService.deleteProgram(programId);
        return ResponseEntity.noContent().build();
    }
}
