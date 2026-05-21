package com.example.clubmanagementbackend.domain.attendance.service;

import com.example.clubmanagementbackend.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email cảnh báo điểm hoạt động thấp.
     * Gọi trực tiếp bằng string — không phụ thuộc vào Member entity.
     *
     * @param toEmail    địa chỉ email người nhận
     * @param memberName tên hiển thị (fullName, hoặc email/memberId nếu không có tên)
     * @param year       năm
     * @param month      tháng
     * @param points     tổng điểm hiện tại
     * @throws Exception nếu gửi thất bại — caller phải bắt exception này
     */
    public void sendLowActivityPointWarning(String toEmail, String memberName,
                                            int year, int month, int points) throws Exception {
        String displayName = (memberName != null && !memberName.trim().isEmpty()) ? memberName : toEmail;

        log.info("Đang gửi email tới {} ({})", displayName, toEmail);

        String subject = String.format("Cảnh báo điểm hoạt động tháng %d/%d", month, year);
        String body = String.format(
                "Xin chào %s,\n" +
                "Điểm hoạt động tháng %d/%d của bạn hiện là %d điểm, dưới mức yêu cầu 10 điểm.\n" +
                "Vui lòng tham gia thêm các hoạt động/chương trình của câu lạc bộ.",
                displayName, month, year, points);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Gửi thành công tới {} ({})", displayName, toEmail);
        } catch (Exception e) {
            log.error("Gửi thất bại tới {} ({}) error: {}", displayName, toEmail, e.getMessage());
            throw e;
        }
    }

    /**
     * Overload giữ backward-compatible với code cũ (dùng Member entity).
     */
    public void sendLowActivityWarning(Member member, int year, int month, int points) throws Exception {
        sendLowActivityPointWarning(member.getEmail(), member.getFullName(), year, month, points);
    }
}
