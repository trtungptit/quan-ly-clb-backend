package com.example.clubmanagementbackend.domain.programparticipation.repository;

import com.example.clubmanagementbackend.domain.programparticipation.entity.ProgramParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgramParticipationRepository extends JpaRepository<ProgramParticipation, Long> {
    List<ProgramParticipation> findByMember_MemberId(String memberId);
    List<ProgramParticipation> findByProgram_ProgramId(String programId);
    Optional<ProgramParticipation> findByMember_MemberIdAndProgram_ProgramId(String memberId, String programId);
}
