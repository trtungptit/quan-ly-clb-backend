package com.example.clubmanagementbackend.domain.account.controller;

import com.example.clubmanagementbackend.domain.account.dto.CreateAccountRequest;
import com.example.clubmanagementbackend.domain.account.dto.UpdateAccountRequest;
import com.example.clubmanagementbackend.domain.account.dto.UserAccountResponse;
import com.example.clubmanagementbackend.domain.account.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @GetMapping
    public ResponseEntity<List<UserAccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(userAccountService.getAllAccounts());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserAccountResponse> getAccountById(@PathVariable String userId) {
        return ResponseEntity.ok(userAccountService.getAccountById(userId));
    }

    @PostMapping
    public ResponseEntity<UserAccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(userAccountService.createAccount(request));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserAccountResponse> updateAccount(
            @PathVariable String userId,
            @RequestBody UpdateAccountRequest request,
            @RequestParam(required = false) String currentUserId) {
        return ResponseEntity.ok(userAccountService.updateAccount(userId, request, currentUserId));
    }
}
