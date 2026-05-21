package com.example.clubmanagementbackend.domain.attendance.repository;

import com.example.clubmanagementbackend.domain.attendance.entity.CheckInSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckInSessionRepository extends JpaRepository<CheckInSession, Long> {
    Optional<CheckInSession> findByToken(String token);
    Optional<CheckInSession> findByActivityIdAndActive(String activityId, boolean active);
    Optional<CheckInSession> findByProgramIdAndActive(String programId, boolean active);
}
