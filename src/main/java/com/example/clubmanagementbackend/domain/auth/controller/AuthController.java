package com.example.clubmanagementbackend.domain.auth.controller;

import com.example.clubmanagementbackend.domain.auth.dto.LoginRequest;
import com.example.clubmanagementbackend.domain.auth.dto.LoginResponse;
import com.example.clubmanagementbackend.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    
    @GetMapping("/me/{userId}")
    public ResponseEntity<LoginResponse> getMe(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getMe(userId));
    }
}
