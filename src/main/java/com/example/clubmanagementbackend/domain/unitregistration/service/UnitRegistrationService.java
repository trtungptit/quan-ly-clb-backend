package com.example.clubmanagementbackend.domain.unitregistration.service;

import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import com.example.clubmanagementbackend.domain.unitregistration.dto.CreateUnitRegistrationRequest;
import com.example.clubmanagementbackend.domain.unitregistration.dto.UnitRegistrationResponse;
import com.example.clubmanagementbackend.domain.unitregistration.dto.UpdateRegistrationStatusRequest;
import com.example.clubmanagementbackend.domain.unitregistration.entity.UnitRegistration;
import com.example.clubmanagementbackend.domain.unitregistration.exception.DuplicateRegistrationException;
import com.example.clubmanagementbackend.domain.unitregistration.repository.UnitRegistrationRepository;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.account.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnitRegistrationService {

    private final UnitRegistrationRepository registrationRepository;
    private final MemberRepository memberRepository;
    private final ClubUnitRepository clubUnitRepository;
    private final MemberUnitRepository memberUnitRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public UnitRegistrationResponse createRegistration(CreateUnitRegistrationRequest request) {
        log.info("Creating unit registration for member {} to unit {}", request.getMemberId(), request.getUnitId());
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> {
                    log.warn("Member not found: {}", request.getMemberId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found");
                });

        ClubUnit unit = clubUnitRepository.findById(request.getUnitId())
                .orElseThrow(() -> {
                    log.warn("Unit not found: {}", request.getUnitId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found");
                });

        // Chỉ chặn khi member đang ACTIVE trong unit — INACTIVE thì vẫn cho gửi đơn lại
        Optional<MemberUnit> existingMemberUnit =
                memberUnitRepository.findByMemberIdAndUnitId(request.getMemberId(), request.getUnitId());
        if (existingMemberUnit.isPresent() && existingMemberUnit.get().getStatus() == Status.ACTIVE) {
            log.warn("Member {} is already ACTIVE in unit {}", request.getMemberId(), request.getUnitId());
            throw new DuplicateRegistrationException("Member is already in this unit.");
        }

        // Chặn nếu đã có đơn PENDING chưa xử lý
        Optional<UnitRegistration> existing = registrationRepository.findByMember_MemberIdAndUnit_UnitId(request.getMemberId(), request.getUnitId());
        if (existing.isPresent() && existing.get().getStatus() == RegistrationStatus.PENDING) {
            log.warn("Member {} has a pending registration for unit {}", request.getMemberId(), request.getUnitId());
            throw new DuplicateRegistrationException("A pending registration already exists for this member and unit.");
        }


        UnitRegistration registration = UnitRegistration.builder()
                .member(member)
                .unit(unit)
                .note(request.getNote())
                .status(RegistrationStatus.PENDING)
                .build();

        UnitRegistration saved = registrationRepository.save(registration);
        log.info("Successfully created unit registration with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public UnitRegistrationResponse updateStatus(Long id, UpdateRegistrationStatusRequest request) {
        log.info("Updating status of unit registration {} to {}", id, request.getStatus());
        UnitRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Unit registration not found: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found");
                });

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            log.warn("Unit registration {} has already been processed with status {}", id, registration.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This registration has already been processed.");
        }

        // 1. Null / rỗng
        if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required.");
        }

        // 2. Parse string → enum (chặn giá trị không tồn tại trong enum)
        RegistrationStatus newStatus;
        try {
            newStatus = RegistrationStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Status must be approved or rejected.");
        }

        // 3. Whitelist: chỉ cho phép approved / rejected
        if (newStatus != RegistrationStatus.APPROVED && newStatus != RegistrationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status must be approved or rejected.");
        }

        registration.setStatus(newStatus);


        if (newStatus == RegistrationStatus.APPROVED) {
            Optional<MemberUnit> existingMemberUnit = memberUnitRepository.findByMemberId(registration.getMember().getMemberId()).stream()
                    .filter(mu -> mu.getUnitId().equals(registration.getUnit().getUnitId()))
                    .findFirst();

            if (existingMemberUnit.isPresent()) {
                MemberUnit mu = existingMemberUnit.get();
                mu.setStatus(Status.ACTIVE);
                memberUnitRepository.save(mu);
                log.info("Activated existing member unit record for member {} in unit {}", mu.getMemberId(), mu.getUnitId());
            } else {
                MemberUnit mu = MemberUnit.builder()
                        .id(UUID.randomUUID().toString())
                        .memberId(registration.getMember().getMemberId())
                        .unitId(registration.getUnit().getUnitId())
                        .position(Position.MEMBER)
                        .status(Status.ACTIVE)
                        .build();
                memberUnitRepository.save(mu);
                log.info("Created new member unit record for member {} in unit {}", mu.getMemberId(), mu.getUnitId());
            }
        }

        UnitRegistration saved = registrationRepository.save(registration);
        log.info("Successfully updated unit registration {} to {}", id, request.getStatus());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UnitRegistrationResponse> getRegistrations(String memberId, String unitId, String managerUserId) {
        List<UnitRegistration> registrations;
        
        if (memberId != null && !memberId.isEmpty() && unitId != null && !unitId.isEmpty()) {
            registrations = registrationRepository.findListByMemberIdAndUnitId(memberId, unitId);
        } else if (memberId != null && !memberId.isEmpty()) {
            registrations = registrationRepository.findByMember_MemberId(memberId);
        } else if (unitId != null && !unitId.isEmpty()) {
            registrations = registrationRepository.findByUnit_UnitId(unitId);
        } else {
            registrations = registrationRepository.findAll();
        }

        if (managerUserId != null && !managerUserId.isEmpty()) {
            UserAccount account = userAccountRepository.findById(managerUserId).orElse(null);
            if (account != null && account.getMember() != null) {
                if (account.getRole() != com.example.clubmanagementbackend.common.enums.Role.CLUB_LEADER) {
                    List<String> managedUnitIds = memberUnitRepository.findByMemberId(account.getMember().getMemberId()).stream()
                            .filter(mu -> mu.getStatus() == Status.ACTIVE && (mu.getPosition() == Position.LEADER || mu.getPosition() == Position.DEPUTY))
                            .map(MemberUnit::getUnitId)
                            .collect(Collectors.toList());
                    registrations = registrations.stream()
                            .filter(r -> managedUnitIds.contains(r.getUnit().getUnitId()))
                            .collect(Collectors.toList());
                }
            }
        }

        return registrations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy toàn bộ đơn đăng ký của một member theo memberId.
     * - Nếu member không tồn tại → 404
     * - Nếu member chưa có đơn nào → trả []
     */
    @Transactional(readOnly = true)
    public List<UnitRegistrationResponse> getRegistrationsByMember(String memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."));

        return registrationRepository.findByMember_MemberId(memberId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private UnitRegistrationResponse mapToResponse(UnitRegistration registration) {
        return UnitRegistrationResponse.builder()
                .id(registration.getId())
                .memberId(registration.getMember() != null ? registration.getMember().getMemberId() : null)
                .memberName(registration.getMember() != null ? registration.getMember().getFullName() : null)
                .unitId(registration.getUnit() != null ? registration.getUnit().getUnitId() : null)
                .unitName(registration.getUnit() != null ? registration.getUnit().getUnitName() : null)
                .unitType(registration.getUnit() != null && registration.getUnit().getType() != null
                        ? registration.getUnit().getType().name().toLowerCase() : null)
                .note(registration.getNote())
                .status(registration.getStatus() != null ? registration.getStatus().name().toLowerCase() : null)
                .createdAt(registration.getCreatedAt())
                .build();
    }
}
