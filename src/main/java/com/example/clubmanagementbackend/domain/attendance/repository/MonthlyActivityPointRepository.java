package com.example.clubmanagementbackend.domain.attendance.repository;

import com.example.clubmanagementbackend.domain.attendance.entity.MonthlyActivityPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyActivityPointRepository extends JpaRepository<MonthlyActivityPoint, Long> {
    Optional<MonthlyActivityPoint> findByMemberIdAndYearAndMonth(String memberId, int year, int month);
    List<MonthlyActivityPoint> findByYearAndMonth(int year, int month);

    @Query("SELECT m FROM MonthlyActivityPoint m WHERE m.year = :year AND m.month = :month AND m.totalPoints < :threshold")
    List<MonthlyActivityPoint> findMembersBelowThreshold(
            @Param("year") int year,
            @Param("month") int month,
            @Param("threshold") int threshold);
}
