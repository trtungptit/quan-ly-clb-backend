package com.example.clubmanagementbackend.domain.achievement.controller;

import com.example.clubmanagementbackend.domain.achievement.dto.AchievementResponse;
import com.example.clubmanagementbackend.domain.achievement.dto.CreateAchievementRequest;
import com.example.clubmanagementbackend.domain.achievement.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public ResponseEntity<List<AchievementResponse>> getAllAchievements(
            @RequestParam(required = false) String programId) {
        return ResponseEntity.ok(achievementService.getAchievements(programId));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<AchievementResponse>> getAchievementsByMemberId(@PathVariable String memberId) {
        return ResponseEntity.ok(achievementService.getAchievementsByMemberId(memberId));
    }

    @PostMapping
    public ResponseEntity<AchievementResponse> createAchievement(@RequestBody CreateAchievementRequest request) {
        return ResponseEntity.ok(achievementService.createAchievement(request));
    }

    @PatchMapping("/{achievementId}")
    public ResponseEntity<AchievementResponse> updateAchievement(
            @PathVariable Long achievementId,
            @RequestBody CreateAchievementRequest request) {
        return ResponseEntity.ok(achievementService.updateAchievement(achievementId, request));
    }

    @DeleteMapping("/{achievementId}")
    public ResponseEntity<Void> deleteAchievement(@PathVariable Long achievementId) {
        achievementService.deleteAchievement(achievementId);
        return ResponseEntity.noContent().build();
    }
}
