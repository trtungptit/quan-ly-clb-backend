package com.example.clubmanagementbackend.domain.achievement.service;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.achievement.dto.AchievementResponse;
import com.example.clubmanagementbackend.domain.achievement.dto.CreateAchievementRequest;
import com.example.clubmanagementbackend.domain.achievement.entity.Achievement;
import com.example.clubmanagementbackend.domain.achievement.repository.AchievementRepository;
import com.example.clubmanagementbackend.domain.annualprogram.entity.AnnualProgram;
import com.example.clubmanagementbackend.domain.annualprogram.repository.AnnualProgramRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.programparticipation.entity.ProgramParticipation;
import com.example.clubmanagementbackend.domain.programparticipation.repository.ProgramParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final ProgramParticipationRepository participationRepository;
    private final MemberRepository memberRepository;
    private final AnnualProgramRepository programRepository;

    public List<AchievementResponse> getAllAchievements() {
        return achievementRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AchievementResponse> getAchievements(String programId) {
        if (programId != null && !programId.isBlank()) {
            return achievementRepository.findByProgramId(programId.trim()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return getAllAchievements();
    }

    public List<AchievementResponse> getAchievementsByMemberId(String memberId) {
        return achievementRepository.findByMemberId(memberId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AchievementResponse createAchievement(CreateAchievementRequest request) {
        if (request.getMemberId() == null || request.getMemberId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member ID is required");
        }

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        AnnualProgram program = null;
        if (request.getProgramId() != null && !request.getProgramId().isBlank()) {
            program = programRepository.findById(request.getProgramId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
        }

        ProgramParticipation participation = null;
        if (request.getParticipationId() != null) {
            participation = participationRepository.findById(request.getParticipationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation not found"));
        }

        // Validate program and participation relations
        if (program != null) {
            if (participation == null) {
                participation = participationRepository.findByMember_MemberIdAndProgram_ProgramId(member.getMemberId(), program.getProgramId())
                        .orElse(null);
            }
            if (participation == null || (participation.getStatus() != RegistrationStatus.APPROVED && participation.getStatus() != RegistrationStatus.JOINED)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thành viên chưa được duyệt tham gia chương trình này.");
            }
        }

        // Enforce duplicate active achievement prevention
        if (program != null) {
            List<Achievement> existingAchievements = achievementRepository.findByMemberId(member.getMemberId());
            boolean duplicate = existingAchievements.stream()
                    .filter(a -> a.getStatus() == Status.ACTIVE)
                    .anyMatch(a -> {
                        if (a.getProgram() != null && a.getProgram().getProgramId().equals(request.getProgramId())) {
                            return true;
                        }
                        if (a.getParticipation() != null && a.getParticipation().getProgram() != null && a.getParticipation().getProgram().getProgramId().equals(request.getProgramId())) {
                            return true;
                        }
                        return false;
                    });
            if (duplicate) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thành viên đã được trao thành tích cho chương trình này rồi.");
            }
        }

        String name = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle()
                : request.getAchievementName();

        Status status = Status.ACTIVE;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                status = Status.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        Achievement achievement = Achievement.builder()
                .achievementName(name)
                .description(request.getDescription())
                .achievementDate(request.getAchievementDate())
                .certificateUrl(request.getCertificateUrl())
                .type(request.getType())
                .status(status)
                .member(member)
                .program(program)
                .participation(participation)
                .createdDate(LocalDateTime.now())
                .build();

        return mapToResponse(achievementRepository.save(achievement));
    }

    @Transactional
    public AchievementResponse updateAchievement(Long achievementId, CreateAchievementRequest request) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));

        String name = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle()
                : request.getAchievementName();

        if (name != null) {
            achievement.setAchievementName(name);
        }
        if (request.getDescription() != null) {
            achievement.setDescription(request.getDescription());
        }
        if (request.getAchievementDate() != null) {
            achievement.setAchievementDate(request.getAchievementDate());
        }
        if (request.getCertificateUrl() != null) {
            achievement.setCertificateUrl(request.getCertificateUrl());
        }
        if (request.getType() != null) {
            achievement.setType(request.getType());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                achievement.setStatus(Status.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        return mapToResponse(achievementRepository.save(achievement));
    }

    @Transactional
    public void deleteAchievement(Long achievementId) {
        if (!achievementRepository.existsById(achievementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found");
        }
        achievementRepository.deleteById(achievementId);
    }

    private AchievementResponse mapToResponse(Achievement a) {
        String memberId = null;
        String memberName = null;
        String programId = null;
        String programName = null;
        Long participationId = null;

        if (a.getMember() != null) {
            memberId = a.getMember().getMemberId();
            memberName = a.getMember().getFullName();
        } else if (a.getParticipation() != null && a.getParticipation().getMember() != null) {
            memberId = a.getParticipation().getMember().getMemberId();
            memberName = a.getParticipation().getMember().getFullName();
        }

        if (a.getParticipation() != null) {
            participationId = a.getParticipation().getParticipationId();
            if (a.getParticipation().getProgram() != null) {
                programId = a.getParticipation().getProgram().getProgramId();
                programName = a.getParticipation().getProgram().getProgramName();
            }
        }

        if (programId == null && a.getProgram() != null) {
            programId = a.getProgram().getProgramId();
            programName = a.getProgram().getProgramName();
        }

        return AchievementResponse.builder()
                .achievementId(a.getAchievementId())
                .participationId(participationId)
                .programId(programId)
                .memberId(memberId)
                .memberName(memberName)
                .fullName(memberName)
                .programName(programName)
                .title(a.getAchievementName())
                .achievementName(a.getAchievementName())
                .description(a.getDescription())
                .achievementDate(a.getAchievementDate())
                .certificateUrl(a.getCertificateUrl())
                .type(a.getType())
                .status(a.getStatus() != null ? a.getStatus().name().toLowerCase() : null)
                .createdDate(a.getCreatedDate())
                .build();
    }
}
