package com.example.clubmanagementbackend.domain.achievement.repository;

import com.example.clubmanagementbackend.domain.achievement.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    
    @Query("SELECT a FROM Achievement a LEFT JOIN a.participation p WHERE a.member.memberId = :memberId OR (p IS NOT NULL AND p.member.memberId = :memberId)")
    List<Achievement> findByMemberId(@Param("memberId") String memberId);

    @Query("SELECT a FROM Achievement a LEFT JOIN a.participation p WHERE a.program.programId = :programId OR (p IS NOT NULL AND p.program.programId = :programId)")
    List<Achievement> findByProgramId(@Param("programId") String programId);
}
