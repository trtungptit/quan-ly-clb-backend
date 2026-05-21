package com.example.clubmanagementbackend.domain.activity.controller;

import com.example.clubmanagementbackend.domain.activity.dto.ActivityRequest;
import com.example.clubmanagementbackend.domain.activity.dto.ActivityResponse;
import com.example.clubmanagementbackend.domain.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getAll(
            @RequestParam(required = false) String unitId,
            @RequestParam(required = false) String managerUserId,
            @RequestParam(required = false) String memberId) {
        return ResponseEntity.ok(activityService.getActivities(unitId, managerUserId, memberId));
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(@RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.createActivity(request));
    }

    @PatchMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable String activityId,
            @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.updateActivity(activityId, request));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable String activityId,
            @RequestParam(required = false) String managerUserId) {
        activityService.deleteActivity(activityId, managerUserId);
        return ResponseEntity.noContent().build();
    }
}
