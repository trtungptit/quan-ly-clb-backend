package com.example.clubmanagementbackend.domain.attendance.controller;

import com.example.clubmanagementbackend.domain.attendance.dto.MonthlyPointResponse;
import com.example.clubmanagementbackend.domain.attendance.dto.MonthlyWarningResponse;
import com.example.clubmanagementbackend.domain.attendance.service.AttendanceService;
import com.example.clubmanagementbackend.domain.attendance.service.MonthlyWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/activity-points")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174",
        "http://127.0.0.1:5173", "http://127.0.0.1:5174"}, allowCredentials = "true")
public class ActivityPointController {

    private final AttendanceService attendanceService;
    private final MonthlyWarningService warningService;

    @GetMapping("/member/{memberId}")
    public ResponseEntity<MonthlyPointResponse> getMemberPoints(
            @PathVariable String memberId,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        LocalDate now = LocalDate.now();
        int y = year == 0 ? now.getYear() : year;
        int m = month == 0 ? now.getMonthValue() : month;
        return ResponseEntity.ok(attendanceService.getMemberMonthlyPoints(memberId, y, m));
    }

    @GetMapping
    public ResponseEntity<List<MonthlyPointResponse>> getAllPoints(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        LocalDate now = LocalDate.now();
        int y = year == 0 ? now.getYear() : year;
        int m = month == 0 ? now.getMonthValue() : month;
        return ResponseEntity.ok(attendanceService.getAllMonthlyPoints(y, m));
    }

    /**
     * Gửi email cảnh báo điểm hoạt động thấp thủ công.
     * Frontend gọi ngay khi bấm nút — không cần đợi cuối tháng.
     *
     * POST /api/activity-points/send-monthly-warning?year=2026&month=5
     */
    @PostMapping("/send-monthly-warning")
    public ResponseEntity<MonthlyWarningResponse> sendMonthlyWarning(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        LocalDate now = LocalDate.now();
        int y = year == 0 ? now.getYear() : year;
        int m = month == 0 ? now.getMonthValue() : month;
        return ResponseEntity.ok(warningService.runMonthlyWarning(y, m));
    }
}
