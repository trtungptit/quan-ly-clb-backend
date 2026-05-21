package com.example.clubmanagementbackend.domain.programparticipation.service;

import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.annualprogram.entity.AnnualProgram;
import com.example.clubmanagementbackend.domain.annualprogram.repository.AnnualProgramRepository;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.programparticipation.dto.CreateProgramParticipationRequest;
import com.example.clubmanagementbackend.domain.programparticipation.dto.ProgramParticipationResponse;
import com.example.clubmanagementbackend.domain.programparticipation.dto.UpdateParticipationStatusRequest;
import com.example.clubmanagementbackend.domain.programparticipation.entity.ProgramParticipation;
import com.example.clubmanagementbackend.domain.programparticipation.repository.ProgramParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramParticipationService {

    private final ProgramParticipationRepository participationRepository;
    private final AnnualProgramRepository programRepository;
    private final MemberRepository memberRepository;
    private final MemberUnitRepository memberUnitRepository;
    private final ClubUnitRepository clubUnitRepository;

    public List<ProgramParticipationResponse> getAllParticipations() {
        return getParticipations(null, null);
    }

    public List<ProgramParticipationResponse> getParticipations(String memberId, String programId) {
        return getParticipations(memberId, programId, null);
    }

    public List<ProgramParticipationResponse> getParticipations(String memberId, String programId, String status) {
        List<ProgramParticipation> participations = participationRepository.findAll();

        if (memberId != null && !memberId.isBlank()) {
            participations = participations.stream()
                    .filter(p -> p.getMember() != null && p.getMember().getMemberId().equals(memberId.trim()))
                    .collect(Collectors.toList());
        }

        if (programId != null && !programId.isBlank()) {
            participations = participations.stream()
                    .filter(p -> p.getProgram() != null && p.getProgram().getProgramId().equals(programId.trim()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            try {
                RegistrationStatus regStatus = RegistrationStatus.valueOf(status.trim().toUpperCase());
                participations = participations.stream()
                        .filter(p -> p.getStatus() == regStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                return List.of();
            }
        }

        return participations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProgramParticipationResponse createParticipation(CreateProgramParticipationRequest request) {
        AnnualProgram program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
        
        if (program.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program is deleted");
        }

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        Optional<ProgramParticipation> existingOpt = participationRepository.findByMember_MemberIdAndProgram_ProgramId(request.getMemberId(), request.getProgramId());
        if (existingOpt.isPresent()) {
            ProgramParticipation existing = existingOpt.get();
            if (existing.getStatus() == RegistrationStatus.PENDING || existing.getStatus() == RegistrationStatus.APPROVED || existing.getStatus() == RegistrationStatus.JOINED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member has already registered for this program");
            } else {
                existing.setStatus(RegistrationStatus.PENDING);
                existing.setNote(request.getNote());
                return mapToResponse(participationRepository.save(existing));
            }
        }

        ProgramParticipation participation = ProgramParticipation.builder()
                .program(program)
                .member(member)
                .note(request.getNote())
                .status(RegistrationStatus.PENDING)
                .createdDate(LocalDateTime.now())
                .build();
        
        return mapToResponse(participationRepository.save(participation));
    }

    @Transactional
    public ProgramParticipationResponse updateStatus(Long participationId, UpdateParticipationStatusRequest request) {
        ProgramParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation not found"));

        if (participation.getStatus() != RegistrationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This participation has already been processed");
        }

        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        RegistrationStatus newStatus;
        try {
            newStatus = RegistrationStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status. Allowed: APPROVED, REJECTED, CANCELLED, JOINED");
        }

        participation.setStatus(newStatus);
        return mapToResponse(participationRepository.save(participation));
    }

    @Transactional
    public ProgramParticipationResponse cancelParticipation(Long participationId) {
        ProgramParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participation not found"));

        if (participation.getStatus() == RegistrationStatus.APPROVED || participation.getStatus() == RegistrationStatus.JOINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel approved or joined participation");
        }

        AnnualProgram program = participation.getProgram();
        LocalDate deadline = program.getCancelDeadline() != null ? program.getCancelDeadline() : program.getRegisterDeadline();
        
        if (deadline != null && LocalDate.now().isAfter(deadline)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancel deadline has passed");
        }

        participation.setStatus(RegistrationStatus.CANCELLED);
        return mapToResponse(participationRepository.save(participation));
    }

    private ProgramParticipationResponse mapToResponse(ProgramParticipation p) {
        Member m = p.getMember();
        AnnualProgram program = p.getProgram();

        String unitId = null;
        String unitName = null;
        String unitType = null;
        String position = null;

        if (m != null) {
            List<MemberUnit> units = memberUnitRepository.findByMemberId(m.getMemberId());
            MemberUnit activeMu = units.stream()
                    .filter(mu -> mu.getStatus() == Status.ACTIVE)
                    .findFirst()
                    .orElse(null);
            if (activeMu != null) {
                unitId = activeMu.getUnitId();
                position = activeMu.getPosition() != null ? activeMu.getPosition().name() : null;
                ClubUnit unit = clubUnitRepository.findById(unitId).orElse(null);
                if (unit != null) {
                    unitName = unit.getUnitName();
                    unitType = unit.getType() != null ? unit.getType().name().toLowerCase() : null;
                }
            }
        }

        return ProgramParticipationResponse.builder()
                .participationId(p.getParticipationId())
                .programId(program != null ? program.getProgramId() : null)
                .programName(program != null ? program.getProgramName() : null)
                .memberId(m != null ? m.getMemberId() : null)
                .memberName(m != null ? m.getFullName() : null)
                .fullName(m != null ? m.getFullName() : null)
                .email(m != null ? m.getEmail() : null)
                .phone(m != null ? m.getPhone() : null)
                .position(position)
                .unitId(unitId)
                .unitName(unitName)
                .unitType(unitType)
                .note(p.getNote())
                .status(p.getStatus() != null ? p.getStatus().name().toLowerCase() : null)
                .createdDate(p.getCreatedDate())
                .createdAt(p.getCreatedDate())
                .build();
    }
}
