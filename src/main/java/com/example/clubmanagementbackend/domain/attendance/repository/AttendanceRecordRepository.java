package com.example.clubmanagementbackend.domain.attendance.repository;

import com.example.clubmanagementbackend.domain.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByMemberIdAndActivityId(String memberId, String activityId);
    Optional<AttendanceRecord> findByMemberIdAndProgramId(String memberId, String programId);
    List<AttendanceRecord> findByMemberId(String memberId);
    List<AttendanceRecord> findByActivityId(String activityId);
    List<AttendanceRecord> findByProgramId(String programId);
}
