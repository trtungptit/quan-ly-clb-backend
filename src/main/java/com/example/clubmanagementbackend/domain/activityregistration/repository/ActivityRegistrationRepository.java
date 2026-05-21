package com.example.clubmanagementbackend.domain.activityregistration.repository;

import com.example.clubmanagementbackend.domain.activityregistration.entity.ActivityRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {
    
    Optional<ActivityRegistration> findByMember_MemberIdAndActivity_ActivityId(String memberId, String activityId);
    
    List<ActivityRegistration> findByMember_MemberId(String memberId);
    
    List<ActivityRegistration> findByActivity_ActivityId(String activityId);
    
    @Query("SELECT a FROM ActivityRegistration a WHERE a.member.memberId = :memberId AND a.activity.activityId = :activityId")
    List<ActivityRegistration> findListByMemberIdAndActivityId(@Param("memberId") String memberId, @Param("activityId") String activityId);
}
