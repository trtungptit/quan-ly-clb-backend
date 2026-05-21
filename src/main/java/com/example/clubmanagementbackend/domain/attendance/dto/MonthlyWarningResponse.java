package com.example.clubmanagementbackend.domain.attendance.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyWarningResponse {

    private boolean success;
    private int year;
    private int month;
    private int threshold;
    private int totalMembersChecked;
    private int lowPointCount;
    private int emailSentCount;
    private int skippedNoEmailCount;
    private int failedEmailCount;
    private int notificationCreatedCount;
    private String message;

    /** Danh sách member đã gửi email thành công */
    @Builder.Default
    private List<SentItem> sent = new ArrayList<>();

    /** Danh sách member bỏ qua vì không có email / chưa cấu hình SMTP */
    @Builder.Default
    private List<SkippedItem> skippedNoEmail = new ArrayList<>();

    /** Danh sách member gửi email thất bại */
    @Builder.Default
    private List<FailedItem> failed = new ArrayList<>();

    // -------- Inner static classes --------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentItem {
        private String memberId;
        private String memberName;
        private String email;
        private int totalPoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedItem {
        private String memberId;
        private String memberName;
        private int totalPoints;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedItem {
        private String memberId;
        private String memberName;
        private String email;
        private int totalPoints;
        private String error;
    }
}
