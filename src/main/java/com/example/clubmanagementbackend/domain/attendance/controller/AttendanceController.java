package com.example.clubmanagementbackend.domain.attendance.controller;

import com.example.clubmanagementbackend.domain.attendance.dto.AttendanceParticipantResponse;
import com.example.clubmanagementbackend.domain.attendance.dto.AttendanceResponse;
import com.example.clubmanagementbackend.domain.attendance.dto.ManualAttendanceRequest;
import com.example.clubmanagementbackend.domain.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller điểm danh thủ công (thay thế luồng QR cũ).
 *
 * Các API QR cũ đã bị XÓA:
 *   POST /api/check-in-sessions/activity/{activityId}  — ĐÃ XÓA
 *   POST /api/check-in-sessions/program/{programId}    — ĐÃ XÓA
 *   GET  /api/check-in-sessions/{token}                — ĐÃ XÓA
 *   POST /api/check-in/{token}                         — ĐÃ XÓA
 *
 * API mới:
 *   GET  /api/attendance/activity/{activityId}                          — danh sách điểm danh hoạt động
 *   POST /api/attendance/activity/{activityId}/members/{memberId}       — điểm danh thủ công hoạt động
 *   GET  /api/attendance/program/{programId}                            — danh sách điểm danh chương trình
 *   POST /api/attendance/program/{programId}/members/{memberId}         — điểm danh thủ công chương trình
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174",
        "http://127.0.0.1:5173", "http://127.0.0.1:5174"}, allowCredentials = "true")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // -------- Activity --------

    /**
     * Lấy danh sách người đã đăng ký được duyệt (APPROVED) cho hoạt động,
     * kèm trạng thái đã điểm danh hay chưa.
     */
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<AttendanceParticipantResponse>> getActivityAttendance(
            @PathVariable String activityId) {
        return ResponseEntity.ok(attendanceService.getActivityAttendanceList(activityId));
    }

    /**
     * Quản lý/Chủ tịch bấm điểm danh thủ công cho member trong hoạt động.
     * force=true: bỏ qua kiểm tra ngày diễn ra (dùng cho demo/test).
     */
    @PostMapping("/activity/{activityId}/members/{memberId}")
    public ResponseEntity<AttendanceResponse> markActivityAttendance(
            @PathVariable String activityId,
            @PathVariable String memberId,
            @RequestBody(required = false) ManualAttendanceRequest request,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        return ResponseEntity.ok(
                attendanceService.markActivityAttendance(activityId, memberId, request, force));
    }

    // -------- Program --------

    /**
     * Lấy danh sách người đã đăng ký được duyệt (APPROVED) cho chương trình thường niên,
     * kèm trạng thái đã điểm danh hay chưa.
     */
    @GetMapping("/program/{programId}")
    public ResponseEntity<List<AttendanceParticipantResponse>> getProgramAttendance(
            @PathVariable String programId) {
        return ResponseEntity.ok(attendanceService.getProgramAttendanceList(programId));
    }

    /**
     * Quản lý/Chủ tịch bấm điểm danh thủ công cho member trong chương trình thường niên.
     * force=true: bỏ qua kiểm tra ngày diễn ra (dùng cho demo/test).
     */
    @PostMapping("/program/{programId}/members/{memberId}")
    public ResponseEntity<AttendanceResponse> markProgramAttendance(
            @PathVariable String programId,
            @PathVariable String memberId,
            @RequestBody(required = false) ManualAttendanceRequest request,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        return ResponseEntity.ok(
                attendanceService.markProgramAttendance(programId, memberId, request, force));
    }
}
