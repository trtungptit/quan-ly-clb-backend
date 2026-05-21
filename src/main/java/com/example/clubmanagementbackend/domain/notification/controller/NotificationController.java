package com.example.clubmanagementbackend.domain.notification.controller;

import com.example.clubmanagementbackend.common.enums.ReadStatus;
import com.example.clubmanagementbackend.domain.notification.dto.CreateNotificationRequest;
import com.example.clubmanagementbackend.domain.notification.dto.NotificationResponse;
import com.example.clubmanagementbackend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Void> createNotification(@RequestBody CreateNotificationRequest request) {
        notificationService.createNotification(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam String memberId,
            @RequestParam(required = false) ReadStatus status) {
        return ResponseEntity.ok(notificationService.getNotificationsForMember(memberId, status));
    }

    @PutMapping("/{id}/mark-read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestParam String memberId) {
        notificationService.markAsRead(id, memberId);
        return ResponseEntity.ok().build();
    }
}
