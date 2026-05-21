package com.example.clubmanagementbackend.domain.unitregistration.repository;

import com.example.clubmanagementbackend.domain.unitregistration.entity.UnitRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRegistrationRepository extends JpaRepository<UnitRegistration, Long> {

    Optional<UnitRegistration> findByMember_MemberIdAndUnit_UnitId(String memberId, String unitId);

    List<UnitRegistration> findByMember_MemberId(String memberId);
    
    List<UnitRegistration> findByUnit_UnitId(String unitId);
    
    @Query("SELECT u FROM UnitRegistration u WHERE u.member.memberId = :memberId AND u.unit.unitId = :unitId")
    List<UnitRegistration> findListByMemberIdAndUnitId(@Param("memberId") String memberId, @Param("unitId") String unitId);
}
