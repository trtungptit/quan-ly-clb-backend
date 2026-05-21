package com.example.clubmanagementbackend.domain.member.repository;

import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, String> {
    List<Member> findByStatus(Status status);
}
