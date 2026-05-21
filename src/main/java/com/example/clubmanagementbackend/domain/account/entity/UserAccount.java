package com.example.clubmanagementbackend.domain.account.entity;

import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {
    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;
}
