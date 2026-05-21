package com.example.clubmanagementbackend.domain.account.repository;

import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<UserAccount> findByMember_MemberId(String memberId);
}
