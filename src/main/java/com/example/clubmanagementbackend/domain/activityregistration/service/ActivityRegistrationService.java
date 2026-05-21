package com.example.clubmanagementbackend.domain.activityregistration.service;

import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.RegistrationStatus;
import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.activity.entity.Activity;
import com.example.clubmanagementbackend.domain.activity.repository.ActivityRepository;
import com.example.clubmanagementbackend.domain.activityregistration.dto.ActivityRegistrationResponse;
import com.example.clubmanagementbackend.domain.activityregistration.dto.CreateActivityRegistrationRequest;
import com.example.clubmanagementbackend.domain.activityregistration.dto.UpdateRegistrationStatusRequest;
import com.example.clubmanagementbackend.domain.activityregistration.entity.ActivityRegistration;
import com.example.clubmanagementbackend.domain.activityregistration.exception.DuplicateRegistrationException;
import com.example.clubmanagementbackend.domain.activityregistration.repository.ActivityRegistrationRepository;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRegistrationService {

    private final ActivityRegistrationRepository registrationRepository;
    private final MemberRepository memberRepository;
    private final ActivityRepository activityRepository;
    private final UserAccountRepository userAccountRepository;
    private final MemberUnitRepository memberUnitRepository;
    private final ClubUnitRepository clubUnitRepository;

    @Transactional
    public ActivityRegistrationResponse createRegistration(CreateActivityRegistrationRequest request) {
        log.info("Creating activity registration for member {} to activity {}", request.getMemberId(), request.getActivityId());
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> {
                    log.warn("Member not found: {}", request.getMemberId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found");
                });

        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> {
                    log.warn("Activity not found: {}", request.getActivityId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found");
                });

        if (activity.getUnitId() == null || activity.getUnitId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hoạt động không thuộc nhóm/ban nào.");
        }

        MemberUnit memberUnit = memberUnitRepository.findByMemberIdAndUnitId(member.getMemberId(), activity.getUnitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thành viên không thuộc nhóm/ban của hoạt động này."));

        if (memberUnit.getStatus() != Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thành viên không hoạt động trong nhóm/ban này.");
        }

        Optional<ActivityRegistration> existingOpt = registrationRepository.findByMember_MemberIdAndActivity_ActivityId(request.getMemberId(), request.getActivityId());
        if (existingOpt.isPresent()) {
            ActivityRegistration existing = existingOpt.get();
            if (existing.getStatus() == RegistrationStatus.PENDING || existing.getStatus() == RegistrationStatus.APPROVED || existing.getStatus() == RegistrationStatus.JOINED) {
                log.warn("Member {} has already registered for activity {} with status {}", request.getMemberId(), request.getActivityId(), existing.getStatus());
                throw new DuplicateRegistrationException("Member has already registered for this activity.");
            } else {
                log.info("Reactivating previous registration for member {} and activity {}", request.getMemberId(), request.getActivityId());
                existing.setStatus(RegistrationStatus.PENDING);
                existing.setNote(request.getNote());
                ActivityRegistration saved = registrationRepository.save(existing);
                return mapToResponse(saved);
            }
        }

        ActivityRegistration registration = ActivityRegistration.builder()
                .member(member)
                .activity(activity)
                .note(request.getNote())
                .status(RegistrationStatus.PENDING)
                .build();

        ActivityRegistration saved = registrationRepository.save(registration);
        log.info("Successfully created activity registration with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public ActivityRegistrationResponse updateStatus(Long id, UpdateRegistrationStatusRequest request) {
        log.info("Updating status of activity registration {} to {}", id, request.getStatus());
        ActivityRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Activity registration not found: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found");
                });

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            log.warn("Activity registration {} has already been processed with status {}", id, registration.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This registration has already been processed.");
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

        registration.setStatus(newStatus);
        ActivityRegistration saved = registrationRepository.save(registration);
        log.info("Successfully updated activity registration {} to {}", id, request.getStatus());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationResponse> getRegistrationsByMember(String memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."));
        return registrationRepository.findByMember_MemberId(memberId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActivityRegistrationResponse cancelRegistration(Long id) {
        ActivityRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity registration not found"));

        if (registration.getStatus() == RegistrationStatus.APPROVED || registration.getStatus() == RegistrationStatus.JOINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel approved or joined activity registration");
        }

        Activity activity = registration.getActivity();
        java.time.LocalDate deadline = activity.getCancelDeadline() != null ? activity.getCancelDeadline() : activity.getRegisterDeadline();
        
        if (deadline != null && java.time.LocalDate.now().isAfter(deadline)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancel deadline has passed");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        return mapToResponse(registrationRepository.save(registration));
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationResponse> getRegistrations(String memberId, String activityId, String managerUserId, String unitId) {
        List<ActivityRegistration> list = registrationRepository.findAll();

        if (memberId != null && !memberId.trim().isEmpty()) {
            list = list.stream()
                    .filter(r -> r.getMember() != null && r.getMember().getMemberId().equals(memberId.trim()))
                    .collect(Collectors.toList());
        }

        if (activityId != null && !activityId.trim().isEmpty()) {
            list = list.stream()
                    .filter(r -> r.getActivity() != null && r.getActivity().getActivityId().equals(activityId.trim()))
                    .collect(Collectors.toList());
        }

        if (unitId != null && !unitId.trim().isEmpty()) {
            list = list.stream()
                    .filter(r -> r.getActivity() != null && unitId.trim().equals(r.getActivity().getUnitId()))
                    .collect(Collectors.toList());
        }

        if (managerUserId != null && !managerUserId.trim().isEmpty()) {
            UserAccount acc = userAccountRepository.findById(managerUserId.trim()).orElse(null);
            if (acc == null) {
                return List.of();
            }
            if (acc.getRole() != Role.CLUB_LEADER && acc.getRole() != Role.ADMIN) {
                if (acc.getMember() == null) {
                    return List.of();
                }
                String managerMemberId = acc.getMember().getMemberId();
                List<String> managedUnitIds = memberUnitRepository.findByMemberId(managerMemberId).stream()
                        .filter(mu -> mu.getStatus() == Status.ACTIVE && 
                                      (mu.getPosition() == Position.LEADER || mu.getPosition() == Position.DEPUTY))
                        .map(MemberUnit::getUnitId)
                        .toList();

                if (managedUnitIds.isEmpty()) {
                    return List.of();
                }
                list = list.stream()
                        .filter(r -> r.getActivity() != null && managedUnitIds.contains(r.getActivity().getUnitId()))
                        .collect(Collectors.toList());
            }
        }

        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private ActivityRegistrationResponse mapToResponse(ActivityRegistration registration) {
        Member m = registration.getMember();
        Activity a = registration.getActivity();
        String activityName = a != null ? a.getActivityName() : null;
        String unitId = a != null ? a.getUnitId() : null;
        String unitName = null;
        String unitType = null;
        if (unitId != null) {
            ClubUnit unit = clubUnitRepository.findById(unitId).orElse(null);
            if (unit != null) {
                unitName = unit.getUnitName();
                unitType = unit.getType() != null ? unit.getType().name().toLowerCase() : null;
            }
        }

        return ActivityRegistrationResponse.builder()
                .id(registration.getId())
                .registrationId(registration.getId() != null ? String.valueOf(registration.getId()) : null)
                .memberId(m != null ? m.getMemberId() : null)
                .memberName(m != null ? m.getFullName() : null)
                .fullName(m != null ? m.getFullName() : null)
                .email(m != null ? m.getEmail() : null)
                .phone(m != null ? m.getPhone() : null)
                .activityId(a != null ? a.getActivityId() : null)
                .activityName(activityName)
                .unitId(unitId)
                .unitName(unitName)
                .unitType(unitType)
                .note(registration.getNote())
                .status(registration.getStatus() != null ? registration.getStatus().name().toLowerCase() : null)
                .createdAt(registration.getCreatedAt())
                .build();
    }
}
