package com.example.clubmanagementbackend.domain.attendance.service;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.activity.entity.Activity;
import com.example.clubmanagementbackend.domain.activity.repository.ActivityRepository;
import com.example.clubmanagementbackend.domain.activityregistration.entity.ActivityRegistration;
import com.example.clubmanagementbackend.domain.activityregistration.repository.ActivityRegistrationRepository;
import com.example.clubmanagementbackend.domain.annualprogram.entity.AnnualProgram;
import com.example.clubmanagementbackend.domain.annualprogram.repository.AnnualProgramRepository;
import com.example.clubmanagementbackend.domain.attendance.dto.*;
import com.example.clubmanagementbackend.domain.attendance.entity.AttendanceRecord;
import com.example.clubmanagementbackend.domain.attendance.entity.MonthlyActivityPoint;
import com.example.clubmanagementbackend.domain.attendance.repository.AttendanceRecordRepository;
import com.example.clubmanagementbackend.domain.attendance.repository.MonthlyActivityPointRepository;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.programparticipation.entity.ProgramParticipation;
import com.example.clubmanagementbackend.domain.programparticipation.repository.ProgramParticipationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final int ACTIVITY_POINTS = 1;
    private static final int PROGRAM_POINTS = 10;
    private static final int POINTS_OK_THRESHOLD = 10;

    private final AttendanceRecordRepository attendanceRepository;
    private final MonthlyActivityPointRepository monthlyPointRepository;
    private final ActivityRepository activityRepository;
    private final AnnualProgramRepository programRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final ProgramParticipationRepository participationRepository;
    private final MemberRepository memberRepository;
    private final MemberUnitRepository memberUnitRepository;
    private final ClubUnitRepository clubUnitRepository;

    // ======================== ACTIVITY - DANH SÁCH ========================

    /**
     * GET /api/attendance/activity/{activityId}
     * Trả danh sách người đã đăng ký được APPROVED, kèm trạng thái điểm danh.
     */
    @Transactional(readOnly = true)
    public List<AttendanceParticipantResponse> getActivityAttendanceList(String activityId) {
        activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hoạt động không tồn tại"));

        List<ActivityRegistration> regs = activityRegistrationRepository
                .findByActivity_ActivityId(activityId)
                .stream()
                .filter(r -> r.getStatus() == RegistrationStatus.APPROVED)
                .collect(Collectors.toList());

        // Build map memberId -> AttendanceRecord
        Map<String, AttendanceRecord> attendedMap = attendanceRepository
                .findByActivityId(activityId)
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getMemberId, r -> r, (a, b) -> a));

        return regs.stream().map(reg -> {
            Member member = reg.getMember();
            String memberId = member.getMemberId();
            String unitName = resolveUnitName(memberId);
            AttendanceRecord record = attendedMap.get(memberId);

            return AttendanceParticipantResponse.builder()
                    .memberId(memberId)
                    .memberName(member.getFullName())
                    .fullName(member.getFullName())
                    .email(member.getEmail())
                    .phone(member.getPhone())
                    .unitName(unitName)
                    .registrationStatus(reg.getStatus().name())
                    .attended(record != null)
                    .checkedInAt(record != null ? record.getCheckedInAt() : null)
                    .pointsAwarded(record != null ? record.getPointsAwarded() : 0)
                    .build();
        }).collect(Collectors.toList());
    }

    // ======================== ACTIVITY - ĐIỂM DANH THỦ CÔNG ========================

    /**
     * POST /api/attendance/activity/{activityId}/members/{memberId}
     * Quản lý bấm điểm danh cho member đã được APPROVED.
     * force=true bỏ qua kiểm tra ngày.
     */
    @Transactional
    public AttendanceResponse markActivityAttendance(String activityId, String memberId,
                                                     ManualAttendanceRequest request, boolean force) {
        // 1. Activity tồn tại
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hoạt động không tồn tại"));

        // 2. Member tồn tại
        memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thành viên không tồn tại"));

        // 3. Member đã đăng ký activity
        ActivityRegistration reg = activityRegistrationRepository
                .findByMember_MemberIdAndActivity_ActivityId(memberId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Thành viên chưa đăng ký hoạt động này"));

        // 4. Registration phải APPROVED
        if (reg.getStatus() != RegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Đơn đăng ký chưa được duyệt (trạng thái: " + reg.getStatus() + ")");
        }

        // 5. Kiểm tra ngày (bỏ qua nếu force=true)
        if (!force) {
            LocalDate today = LocalDate.now();
            if (activity.getStartDate() != null && activity.getEndDate() != null) {
                if (today.isBefore(activity.getStartDate()) || today.isAfter(activity.getEndDate())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Hôm nay không phải ngày diễn ra hoạt động (" +
                                    activity.getStartDate() + " - " + activity.getEndDate() + ")");
                }
            }
        }

        // 6. Kiểm tra unit (nếu activity thuộc unit nào đó)
        if (activity.getUnitId() != null) {
            boolean inUnit = memberUnitRepository.findByMemberIdAndUnitId(memberId, activity.getUnitId())
                    .filter(mu -> mu.getStatus() == Status.ACTIVE)
                    .isPresent();
            if (!inUnit) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Thành viên không thuộc nhóm/ban của hoạt động này");
            }
        }

        // 7. Kiểm tra chưa điểm danh (chống trùng)
        if (attendanceRepository.findByMemberIdAndActivityId(memberId, activityId).isPresent()) {
            int currentPoints = getCurrentMonthPoints(memberId);
            return AttendanceResponse.builder()
                    .success(false)
                    .message("Thành viên đã điểm danh hoạt động này")
                    .memberId(memberId)
                    .activityId(activityId)
                    .pointsAwarded(0)
                    .totalMonthlyPoints(currentPoints)
                    .build();
        }

        // 8. Tạo AttendanceRecord
        LocalDateTime now = LocalDateTime.now();
        AttendanceRecord record = AttendanceRecord.builder()
                .memberId(memberId)
                .activityId(activityId)
                .registrationId(reg.getId())
                .attendanceType("ACTIVITY")
                .unitId(activity.getUnitId())
                .pointsAwarded(ACTIVITY_POINTS)
                .checkedInAt(now)
                .checkInDate(now.toLocalDate())
                .checkedByUserId(request != null ? request.getCheckedByUserId() : null)
                .source("MANUAL")
                .status("CHECKED_IN")
                .build();
        attendanceRepository.save(record);

        // Cộng điểm tháng
        int newTotal = addMonthlyPoints(memberId, now, ACTIVITY_POINTS);
        log.info("Manual check-in: member {} -> activity {} | +{} point | monthly total: {}",
                memberId, activityId, ACTIVITY_POINTS, newTotal);

        return AttendanceResponse.builder()
                .success(true)
                .message("Điểm danh thành công")
                .memberId(memberId)
                .activityId(activityId)
                .pointsAwarded(ACTIVITY_POINTS)
                .totalMonthlyPoints(newTotal)
                .checkedInAt(now)
                .build();
    }

    // ======================== PROGRAM - DANH SÁCH ========================

    /**
     * GET /api/attendance/program/{programId}
     * Trả danh sách người đã APPROVED tham gia chương trình, kèm trạng thái điểm danh.
     */
    @Transactional(readOnly = true)
    public List<AttendanceParticipantResponse> getProgramAttendanceList(String programId) {
        programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chương trình không tồn tại"));

        List<ProgramParticipation> participations = participationRepository
                .findByProgram_ProgramId(programId)
                .stream()
                .filter(p -> p.getStatus() == RegistrationStatus.APPROVED)
                .collect(Collectors.toList());

        // Build map memberId -> AttendanceRecord
        Map<String, AttendanceRecord> attendedMap = attendanceRepository
                .findByProgramId(programId)
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getMemberId, r -> r, (a, b) -> a));

        return participations.stream().map(p -> {
            Member member = p.getMember();
            String memberId = member.getMemberId();
            String unitName = resolveUnitName(memberId);
            AttendanceRecord record = attendedMap.get(memberId);

            return AttendanceParticipantResponse.builder()
                    .memberId(memberId)
                    .memberName(member.getFullName())
                    .fullName(member.getFullName())
                    .email(member.getEmail())
                    .phone(member.getPhone())
                    .unitName(unitName)
                    .participationStatus(p.getStatus().name())
                    .attended(record != null)
                    .checkedInAt(record != null ? record.getCheckedInAt() : null)
                    .pointsAwarded(record != null ? record.getPointsAwarded() : 0)
                    .build();
        }).collect(Collectors.toList());
    }

    // ======================== PROGRAM - ĐIỂM DANH THỦ CÔNG ========================

    /**
     * POST /api/attendance/program/{programId}/members/{memberId}
     * Quản lý bấm điểm danh chương trình thường niên cho member đã APPROVED.
     * force=true bỏ qua kiểm tra ngày.
     */
    @Transactional
    public AttendanceResponse markProgramAttendance(String programId, String memberId,
                                                    ManualAttendanceRequest request, boolean force) {
        // 1. AnnualProgram tồn tại
        AnnualProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chương trình không tồn tại"));

        // 2. Member tồn tại
        memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thành viên không tồn tại"));

        // 3. Member đã đăng ký chương trình
        ProgramParticipation participation = participationRepository
                .findByMember_MemberIdAndProgram_ProgramId(memberId, programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Thành viên chưa đăng ký chương trình này"));

        // 4. Participation phải APPROVED
        if (participation.getStatus() != RegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Đơn đăng ký chưa được duyệt (trạng thái: " + participation.getStatus() + ")");
        }

        // 5. Kiểm tra ngày (bỏ qua nếu force=true)
        if (!force) {
            LocalDate today = LocalDate.now();
            if (program.getStartDate() != null && program.getEndDate() != null) {
                if (today.isBefore(program.getStartDate()) || today.isAfter(program.getEndDate())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Hôm nay không phải ngày diễn ra chương trình (" +
                                    program.getStartDate() + " - " + program.getEndDate() + ")");
                }
            }
        }

        // 6. Kiểm tra chưa điểm danh (chống trùng)
        if (attendanceRepository.findByMemberIdAndProgramId(memberId, programId).isPresent()) {
            int currentPoints = getCurrentMonthPoints(memberId);
            return AttendanceResponse.builder()
                    .success(false)
                    .message("Thành viên đã điểm danh chương trình này")
                    .memberId(memberId)
                    .programId(programId)
                    .pointsAwarded(0)
                    .totalMonthlyPoints(currentPoints)
                    .build();
        }

        // 7. Tạo AttendanceRecord
        LocalDateTime now = LocalDateTime.now();
        AttendanceRecord record = AttendanceRecord.builder()
                .memberId(memberId)
                .programId(programId)
                .participationId(participation.getParticipationId())
                .attendanceType("PROGRAM")
                .pointsAwarded(PROGRAM_POINTS)
                .checkedInAt(now)
                .checkInDate(now.toLocalDate())
                .checkedByUserId(request != null ? request.getCheckedByUserId() : null)
                .source("MANUAL")
                .status("CHECKED_IN")
                .build();
        attendanceRepository.save(record);

        // Cộng điểm tháng
        int newTotal = addMonthlyPoints(memberId, now, PROGRAM_POINTS);
        log.info("Manual check-in: member {} -> program {} | +{} points | monthly total: {}",
                memberId, programId, PROGRAM_POINTS, newTotal);

        return AttendanceResponse.builder()
                .success(true)
                .message("Điểm danh thành công")
                .memberId(memberId)
                .programId(programId)
                .pointsAwarded(PROGRAM_POINTS)
                .totalMonthlyPoints(newTotal)
                .checkedInAt(now)
                .build();
    }

    // ======================== MONTHLY POINTS ========================

    @Transactional(readOnly = true)
    public MonthlyPointResponse getMemberMonthlyPoints(String memberId, int year, int month) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        int total = monthlyPointRepository
                .findByMemberIdAndYearAndMonth(memberId, year, month)
                .map(MonthlyActivityPoint::getTotalPoints)
                .orElse(0);

        String unitName = resolveUnitName(memberId);

        return MonthlyPointResponse.builder()
                .memberId(memberId)
                .memberName(member.getFullName())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .unitName(unitName)
                .year(year)
                .month(month)
                .totalPoints(total)
                .status(total >= POINTS_OK_THRESHOLD ? "OK" : "LOW")
                .build();
    }

    @Transactional(readOnly = true)
    public List<MonthlyPointResponse> getAllMonthlyPoints(int year, int month) {
        List<MonthlyActivityPoint> points = monthlyPointRepository.findByYearAndMonth(year, month);
        return points.stream().map(p -> {
            Member member = memberRepository.findById(p.getMemberId()).orElse(null);
            String name = member != null ? member.getFullName() : p.getMemberId();
            String email = member != null ? member.getEmail() : null;
            String unitName = resolveUnitName(p.getMemberId());
            int total = p.getTotalPoints();

            return MonthlyPointResponse.builder()
                    .memberId(p.getMemberId())
                    .memberName(name)
                    .fullName(name)
                    .email(email)
                    .unitName(unitName)
                    .year(year)
                    .month(month)
                    .totalPoints(total)
                    .status(total >= POINTS_OK_THRESHOLD ? "OK" : "LOW")
                    .build();
        }).collect(Collectors.toList());
    }

    // ======================== HELPER ========================

    private int addMonthlyPoints(String memberId, LocalDateTime dateTime, int points) {
        int year = dateTime.getYear();
        int month = dateTime.getMonthValue();
        Optional<MonthlyActivityPoint> existing =
                monthlyPointRepository.findByMemberIdAndYearAndMonth(memberId, year, month);

        MonthlyActivityPoint record;
        if (existing.isPresent()) {
            record = existing.get();
            record.setTotalPoints(record.getTotalPoints() + points);
            record.setUpdatedAt(LocalDateTime.now());
        } else {
            record = MonthlyActivityPoint.builder()
                    .memberId(memberId)
                    .year(year)
                    .month(month)
                    .totalPoints(points)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        monthlyPointRepository.save(record);
        return record.getTotalPoints();
    }

    private int getCurrentMonthPoints(String memberId) {
        LocalDate now = LocalDate.now();
        return monthlyPointRepository
                .findByMemberIdAndYearAndMonth(memberId, now.getYear(), now.getMonthValue())
                .map(MonthlyActivityPoint::getTotalPoints)
                .orElse(0);
    }

    /**
     * Lấy tên unit chính (ACTIVE) của member, hoặc null nếu không có.
     */
    private String resolveUnitName(String memberId) {
        return memberUnitRepository.findByMemberId(memberId).stream()
                .filter(mu -> mu.getStatus() == Status.ACTIVE)
                .findFirst()
                .map(MemberUnit::getUnitId)
                .flatMap(unitId -> clubUnitRepository.findById(unitId))
                .map(ClubUnit::getUnitName)
                .orElse(null);
    }
}
