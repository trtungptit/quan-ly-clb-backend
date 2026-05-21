package com.example.clubmanagementbackend.domain.auth.service;

import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.auth.dto.LoginRequest;
import com.example.clubmanagementbackend.domain.auth.dto.LoginResponse;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;

    public LoginResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        if (!account.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        if (account.getStatus() != Status.ACTIVE) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        return mapToLoginResponse(account);
    }
    
    public LoginResponse getMe(String userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
        return mapToLoginResponse(account);
    }

    private LoginResponse mapToLoginResponse(UserAccount account) {
        Member member = account.getMember();
        return LoginResponse.builder()
                .userId(account.getUserId())
                .memberId(member != null ? member.getMemberId() : null)
                .username(account.getUsername())
                .role(account.getRole() != null ? account.getRole().name().toLowerCase() : null)
                .status(account.getStatus() != null ? account.getStatus().name().toLowerCase() : null)
                .fullName(member != null ? member.getFullName() : null)
                .email(member != null ? member.getEmail() : null)
                .phone(member != null ? member.getPhone() : null)
                .gender(member != null ? member.getGender() : null)
                .dateOfBirth(member != null ? member.getDateOfBirth() : null)
                .build();
    }
}
