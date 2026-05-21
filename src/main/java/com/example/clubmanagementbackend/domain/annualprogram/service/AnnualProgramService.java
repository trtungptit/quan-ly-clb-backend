package com.example.clubmanagementbackend.domain.annualprogram.service;

import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.annualprogram.dto.AnnualProgramResponse;
import com.example.clubmanagementbackend.domain.annualprogram.dto.CreateAnnualProgramRequest;
import com.example.clubmanagementbackend.domain.annualprogram.entity.AnnualProgram;
import com.example.clubmanagementbackend.domain.annualprogram.repository.AnnualProgramRepository;
import com.example.clubmanagementbackend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnualProgramService {
    private final AnnualProgramRepository programRepository;
    private final NotificationService notificationService;

    public List<AnnualProgramResponse> getAllPrograms() {
        return programRepository.findByDeletedFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AnnualProgramResponse> getCompletedPrograms() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return programRepository.findByDeletedFalse().stream()
                .filter(p -> p.getEndDate() != null && p.getEndDate().isBefore(today))
                .filter(p -> p.getStatus() == Status.ACTIVE)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnnualProgramResponse createProgram(CreateAnnualProgramRequest request) {
        String name = request.getName() != null && !request.getName().isBlank()
                ? request.getName()
                : request.getProgramName();

        Status status = Status.ACTIVE;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                status = Status.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        AnnualProgram program = AnnualProgram.builder()
                .programId(UUID.randomUUID().toString())
                .programName(name)
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .registerDeadline(request.getRegisterDeadline())
                .cancelDeadline(request.getCancelDeadline())
                .location(request.getLocation())
                .maxParticipants(request.getMaxParticipants())
                .year(request.getYear())
                .status(status)
                .deleted(false)
                .build();
        
        AnnualProgram savedProgram = programRepository.save(program);
        try {
            String title = "Chương trình thường niên mới";
            String content = String.format("Chương trình %s đã được tạo. Hạn đăng ký: %s", 
                    savedProgram.getProgramName(), 
                    savedProgram.getRegisterDeadline() != null ? savedProgram.getRegisterDeadline().toString() : "N/A");
            notificationService.createNotificationForAllActiveMembers(
                    title, content, "ANNUAL_PROGRAM", savedProgram.getProgramId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapToResponse(savedProgram);
    }

    @Transactional
    public AnnualProgramResponse updateProgram(String programId, CreateAnnualProgramRequest request) {
        AnnualProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        if (program.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program is deleted");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            program.setProgramName(request.getName());
        } else if (request.getProgramName() != null) {
            program.setProgramName(request.getProgramName());
        }

        if (request.getDescription() != null) {
            program.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            program.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            program.setEndDate(request.getEndDate());
        }
        if (request.getRegisterDeadline() != null) {
            program.setRegisterDeadline(request.getRegisterDeadline());
        }
        if (request.getCancelDeadline() != null) {
            program.setCancelDeadline(request.getCancelDeadline());
        }
        if (request.getLocation() != null) {
            program.setLocation(request.getLocation());
        }
        if (request.getMaxParticipants() != null) {
            program.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getYear() != null) {
            program.setYear(request.getYear());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                program.setStatus(Status.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        return mapToResponse(programRepository.save(program));
    }

    @Transactional
    public void deleteProgram(String programId) {
        AnnualProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
        program.setDeleted(true);
        programRepository.save(program);
    }

    private AnnualProgramResponse mapToResponse(AnnualProgram p) {
        return AnnualProgramResponse.builder()
                .programId(p.getProgramId())
                .programName(p.getProgramName())
                .name(p.getProgramName())
                .description(p.getDescription())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .registerDeadline(p.getRegisterDeadline())
                .registrationDeadline(p.getRegisterDeadline())
                .cancelDeadline(p.getCancelDeadline())
                .location(p.getLocation())
                .maxParticipants(p.getMaxParticipants())
                .year(p.getYear())
                .status(p.getStatus() != null ? p.getStatus().name().toLowerCase() : null)
                .build();
    }
}
