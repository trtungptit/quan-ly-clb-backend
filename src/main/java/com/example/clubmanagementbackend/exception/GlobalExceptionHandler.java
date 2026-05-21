package com.example.clubmanagementbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Global exception handler – trả về JSON { "message": "..." } thay vì HTML "Bad Request".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt ResponseStatusException (400, 403, 404, ...) ném từ service.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of("message", ex.getReason() != null ? ex.getReason() : ex.getMessage()));
    }

    /**
     * Bắt lỗi khi Jackson không đọc được request body (ví dụ: sai kiểu dữ liệu, JSON malformed).
     * Trả 400 với thông báo rõ ràng thay vì Spring default HTML.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        String msg = ex.getMessage();
        // Lấy phần đầu trước dấu \n để tránh stacktrace dài
        if (msg != null && msg.contains("\n")) {
            msg = msg.substring(0, msg.indexOf('\n'));
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Request body không hợp lệ: " + msg));
    }

    /**
     * Bắt IllegalArgumentException (ví dụ: Enum.valueOf sai).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Fallback: bắt mọi exception chưa xử lý.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Lỗi hệ thống: " + ex.getMessage()));
    }
}
