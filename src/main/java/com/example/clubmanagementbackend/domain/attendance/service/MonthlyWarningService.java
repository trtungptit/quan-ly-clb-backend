package com.example.clubmanagementbackend.domain.attendance.service;

import com.example.clubmanagementbackend.common.enums.ReadStatus;
import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.attendance.dto.MonthlyWarningResponse;
import com.example.clubmanagementbackend.domain.attendance.dto.MonthlyWarningResponse.FailedItem;
import com.example.clubmanagementbackend.domain.attendance.dto.MonthlyWarningResponse.SkippedItem;
import com.example.clubmanagementbackend.domain.attendance.dto.MonthlyWarningResponse.SentItem;
import com.example.clubmanagementbackend.domain.attendance.entity.MonthlyActivityPoint;
import com.example.clubmanagementbackend.domain.attendance.repository.MonthlyActivityPointRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.notification.entity.Notification;
import com.example.clubmanagementbackend.domain.notification.entity.NotificationReceiver;
import com.example.clubmanagementbackend.domain.notification.repository.NotificationReceiverRepository;
import com.example.clubmanagementbackend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyWarningService {

    private static final int POINTS_THRESHOLD = 10;
    private static final String WARNING_TITLE = "Cảnh báo điểm hoạt động";
    private static final String WARNING_TYPE  = "MONTHLY_WARNING";

    private final MemberRepository memberRepository;
    private final MonthlyActivityPointRepository monthlyPointRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationReceiverRepository notificationReceiverRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final UserAccountRepository userAccountRepository;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    // ======================== SCHEDULER ========================

    /**
     * Chạy mỗi ngày lúc 23:59.
     * Nếu là ngày cuối tháng → tự động gửi cảnh báo.
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void sendMonthlyWarningIfLastDay() {
        LocalDate today = LocalDate.now();
        LocalDate lastDay = YearMonth.of(today.getYear(), today.getMonth()).atEndOfMonth();
        if (today.equals(lastDay)) {
            log.info("Last day of month — auto-running monthly warning for {}/{}", today.getMonthValue(), today.getYear());
            runMonthlyWarning(today.getYear(), today.getMonthValue());
        }
    }

    // ======================== CORE — được gọi từ API ========================

    /**
     * Gửi cảnh báo điểm hoạt động tháng thủ công.
     * Frontend gọi ngay khi bấm nút, không cần đợi cuối tháng.
     *
     * <p>Với mỗi member active:
     * <ol>
     *   <li>Lấy điểm tháng (mặc định 0 nếu chưa có bản ghi).</li>
     *   <li>Nếu totalPoints {@code >= 10}: bỏ qua.</li>
     *   <li>Nếu totalPoints {@code < 10}:
     *     <ul>
     *       <li>Tạo in-app notification (DB + WebSocket push).</li>
     *       <li>Gửi email nếu SMTP được cấu hình và member có email.</li>
     *     </ul>
     *   </li>
     * </ol>
     */
    @Transactional
    public MonthlyWarningResponse runMonthlyWarning(int year, int month) {
        List<Member> activeMembers = memberRepository.findByStatus(Status.ACTIVE);
        boolean mailConfigured = isMailConfigured();

        log.info("Manual monthly warning {}/{}: {} active members, mailConfigured={}",
                month, year, activeMembers.size(), mailConfigured);

        if (!mailConfigured) {
            log.warn("MAIL_USERNAME / MAIL_PASSWORD not set — SMTP email not configured");
            return MonthlyWarningResponse.builder()
                    .success(false)
                    .year(year)
                    .month(month)
                    .threshold(POINTS_THRESHOLD)
                    .totalMembersChecked(activeMembers.size())
                    .message("Chưa cấu hình SMTP email")
                    .build();
        }

        List<SentItem>    sentList    = new ArrayList<>();
        List<SkippedItem> skippedList = new ArrayList<>();
        List<FailedItem>  failedList  = new ArrayList<>();
        int notificationCreatedCount  = 0;

        for (Member member : activeMembers) {
            // Loại bỏ Chủ tịch/Manager/Admin (chỉ gửi cho member thường)
            Optional<UserAccount> accountOpt = userAccountRepository.findByMember_MemberId(member.getMemberId());
            if (accountOpt.isPresent()) {
                Role role = accountOpt.get().getRole();
                if (role != Role.MEMBER) {
                    log.info("Bỏ qua thành viên lãnh đạo/admin {} có vai trò {}", member.getMemberId(), role);
                    continue;
                }
            }

            int totalPoints = monthlyPointRepository
                    .findByMemberIdAndYearAndMonth(member.getMemberId(), year, month)
                    .map(MonthlyActivityPoint::getTotalPoints)
                    .orElse(0);

            // Đủ điểm — bỏ qua
            if (totalPoints >= POINTS_THRESHOLD) {
                continue;
            }

            String displayName = (member.getFullName() != null && !member.getFullName().isBlank())
                    ? member.getFullName()
                    : member.getMemberId();

            String notifContent = String.format(
                    "Điểm hoạt động tháng %d/%d của bạn hiện là %d điểm, dưới mức yêu cầu 10 điểm.",
                    month, year, totalPoints);

            // --- 1. In-app notification (DB) ---
            try {
                Notification notification = Notification.builder()
                        .title(WARNING_TITLE)
                        .content(notifContent)
                        .type(WARNING_TYPE)
                        .relatedId(String.format("%d-%02d", year, month))
                        .build();
                Notification saved = notificationRepository.save(notification);

                NotificationReceiver receiver = NotificationReceiver.builder()
                        .notification(saved)
                        .member(member)
                        .readStatus(ReadStatus.UNREAD)
                        .build();
                notificationReceiverRepository.save(receiver);
                notificationCreatedCount++;

                // --- 2. WebSocket push ---
                try {
                    Object wsPayload = buildWsPayload(saved, member, receiver);
                    messagingTemplate.convertAndSend(
                            "/topic/notifications/" + member.getMemberId(), wsPayload);
                } catch (Exception wsEx) {
                    log.warn("WebSocket push failed for member {}: {}", member.getMemberId(), wsEx.getMessage());
                }

            } catch (Exception dbEx) {
                log.error("Failed to create in-app notification for member {}: {}", member.getMemberId(), dbEx.getMessage());
            }

            // --- 3. Email ---
            String email = member.getEmail();
            boolean hasEmail = email != null && !email.trim().isEmpty();

            if (!hasEmail) {
                log.warn("Thành viên {} không có email, bỏ qua", member.getMemberId());
                skippedList.add(SkippedItem.builder()
                        .memberId(member.getMemberId())
                        .memberName(displayName)
                        .totalPoints(totalPoints)
                        .reason("Không có email")
                        .build());
            } else {
                try {
                    emailService.sendLowActivityPointWarning(email, displayName, year, month, totalPoints);
                    sentList.add(SentItem.builder()
                            .memberId(member.getMemberId())
                            .memberName(displayName)
                            .email(email)
                            .totalPoints(totalPoints)
                            .build());
                } catch (Exception mailEx) {
                    log.warn("Gửi email thất bại tới {} ({}) error: {}", member.getMemberId(), email, mailEx.getMessage());
                    failedList.add(FailedItem.builder()
                            .memberId(member.getMemberId())
                            .memberName(displayName)
                            .email(email)
                            .totalPoints(totalPoints)
                            .error(mailEx.getMessage())
                            .build());
                }
            }
        }

        int lowPointCount = sentList.size() + skippedList.size() + failedList.size();
        String message;
        if (lowPointCount == 0) {
            message = "Không có thành viên nào dưới " + POINTS_THRESHOLD + " điểm trong tháng này";
        } else {
            message = String.format("Đã gửi email cảnh báo cho %d thành viên dưới %d điểm", sentList.size(), POINTS_THRESHOLD);
        }

        log.info("Monthly warning done: lowPoint={}, sent={}, skipped={}, failed={}, notifCreated={}",
                lowPointCount, sentList.size(), skippedList.size(), failedList.size(), notificationCreatedCount);

        return MonthlyWarningResponse.builder()
                .success(true)
                .year(year)
                .month(month)
                .threshold(POINTS_THRESHOLD)
                .totalMembersChecked(activeMembers.size())
                .lowPointCount(lowPointCount)
                .emailSentCount(sentList.size())
                .skippedNoEmailCount(skippedList.size())
                .failedEmailCount(failedList.size())
                .notificationCreatedCount(notificationCreatedCount)
                .message(message)
                .sent(sentList)
                .skippedNoEmail(skippedList)
                .failed(failedList)
                .build();
    }

    // ======================== HELPERS ========================

    private boolean isMailConfigured() {
        return mailUsername != null && !mailUsername.trim().isEmpty()
                && mailPassword != null && !mailPassword.trim().isEmpty();
    }

    private Map<String, Object> buildWsPayload(Notification n, Member m, NotificationReceiver r) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", r.getId());
        p.put("notificationId", n.getId());
        p.put("memberId", m.getMemberId());
        p.put("title", n.getTitle());
        p.put("content", n.getContent());
        p.put("type", n.getType());
        p.put("relatedId", n.getRelatedId());
        p.put("readStatus", "unread");
        p.put("isRead", false);
        p.put("read", false);
        p.put("createdAt", n.getCreatedAt());
        return p;
    }
}
