package com.example.clubmanagementbackend.domain.activity.service;

import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.activity.dto.ActivityRequest;
import com.example.clubmanagementbackend.domain.activity.dto.ActivityResponse;
import com.example.clubmanagementbackend.domain.activity.entity.Activity;
import com.example.clubmanagementbackend.domain.activity.repository.ActivityRepository;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClubUnitRepository clubUnitRepository;
    private final MemberUnitRepository memberUnitRepository;

    public List<ActivityResponse> getActivities(String unitId, String managerUserId, String memberId) {
        List<Activity> list;
        
        if (unitId != null && !unitId.isBlank()) {
            list = activityRepository.findByUnitId(unitId.trim());
        } else if (managerUserId != null && !managerUserId.isBlank()) {
            UserAccount acc = userAccountRepository.findById(managerUserId.trim()).orElse(null);
            if (acc == null) {
                return List.of();
            }
            if (acc.getRole() == Role.CLUB_LEADER || acc.getRole() == Role.ADMIN) {
                list = activityRepository.findAll();
            } else {
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
                list = activityRepository.findByUnitIdIn(managedUnitIds);
            }
        } else if (memberId != null && !memberId.isBlank()) {
            List<String> memberUnitIds = memberUnitRepository.findByMemberId(memberId.trim()).stream()
                    .filter(mu -> mu.getStatus() == Status.ACTIVE)
                    .map(MemberUnit::getUnitId)
                    .toList();
            if (memberUnitIds.isEmpty()) {
                return List.of();
            }
            list = activityRepository.findByUnitIdIn(memberUnitIds);
        } else {
            list = activityRepository.findAll();
        }
        
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        String effectiveUnitId = request.getEffectiveUnitId();
        String effectiveManagerUserId = request.getEffectiveManagerUserId();

        if ((effectiveUnitId == null || effectiveUnitId.isBlank()) && effectiveManagerUserId != null && !effectiveManagerUserId.isBlank()) {
            UserAccount acc = userAccountRepository.findById(effectiveManagerUserId.trim()).orElse(null);
            if (acc != null && acc.getMember() != null) {
                String memberId = acc.getMember().getMemberId();
                Optional<MemberUnit> managedUnit = memberUnitRepository.findByMemberId(memberId).stream()
                        .filter(mu -> mu.getStatus() == Status.ACTIVE && 
                                      (mu.getPosition() == Position.LEADER || mu.getPosition() == Position.DEPUTY))
                        .findFirst();
                if (managedUnit.isPresent()) {
                    effectiveUnitId = managedUnit.get().getUnitId();
                }
            }
        }

        if (effectiveUnitId == null || effectiveUnitId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn vị (unitId) không được để trống.");
        }

        checkManagerAccess(effectiveManagerUserId, effectiveUnitId);

        String actName = request.getName() != null && !request.getName().isBlank()
                ? request.getName()
                : request.getActivityName();

        Status status = Status.ACTIVE;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                status = Status.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        Activity a = Activity.builder()
                .activityId(UUID.randomUUID().toString())
                .activityName(actName)
                .unitId(effectiveUnitId)
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .registerDeadline(request.getRegisterDeadline())
                .cancelDeadline(request.getCancelDeadline())
                .location(request.getLocation())
                .isLimited(request.isLimited())
                .maxParticipants(request.getMaxParticipants())
                .createdBy(effectiveManagerUserId)
                .status(status)
                .createdDate(LocalDate.now())
                .build();
        return mapToResponse(activityRepository.save(a));
    }

    @Transactional
    public ActivityResponse updateActivity(String activityId, ActivityRequest request) {
        Activity a = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        
        String effectiveManagerUserId = request.getEffectiveManagerUserId();
        checkManagerAccess(effectiveManagerUserId, a.getUnitId());

        if (request.getName() != null && !request.getName().isBlank()) {
            a.setActivityName(request.getName());
        } else if (request.getActivityName() != null) {
            a.setActivityName(request.getActivityName());
        }

        a.setDescription(request.getDescription());
        a.setStartDate(request.getStartDate());
        a.setEndDate(request.getEndDate());
        a.setRegisterDeadline(request.getRegisterDeadline());
        a.setCancelDeadline(request.getCancelDeadline());
        a.setLocation(request.getLocation());
        a.setLimited(request.isLimited());
        a.setMaxParticipants(request.getMaxParticipants());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                a.setStatus(Status.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        return mapToResponse(activityRepository.save(a));
    }

    @Transactional
    public void deleteActivity(String activityId, String managerUserId) {
        Activity a = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        
        checkManagerAccess(managerUserId, a.getUnitId());
        
        activityRepository.delete(a);
    }

    private void checkManagerAccess(String managerUserId, String unitId) {
        if (managerUserId == null || managerUserId.isBlank()) {
            log.warn("Bypassing manager access check: managerUserId is missing");
            return;
        }
        UserAccount acc = userAccountRepository.findById(managerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (acc.getRole() == Role.CLUB_LEADER) {
            return;
        }

        if (acc.getMember() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is not linked to any member");
        }

        if (unitId == null || unitId.isBlank()) {
            log.warn("Bypassing unit-specific manager check: unitId is missing");
            return;
        }

        MemberUnit mu = memberUnitRepository.findByMemberIdAndUnitId(acc.getMember().getMemberId(), unitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this unit"));
        
        if (mu.getPosition() != Position.LEADER && mu.getPosition() != Position.DEPUTY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a manager of this unit");
        }
    }

    private ActivityResponse mapToResponse(Activity a) {
        ClubUnit unit = null;
        if (a.getUnitId() != null) {
            unit = clubUnitRepository.findById(a.getUnitId()).orElse(null);
        }
        
        return ActivityResponse.builder()
                .activityId(a.getActivityId())
                .activityName(a.getActivityName())
                .name(a.getActivityName())
                .unitId(a.getUnitId())
                .unitName(unit != null ? unit.getUnitName() : null)
                .unitType(unit != null && unit.getType() != null ? unit.getType().name().toLowerCase() : null)
                .groupId(unit != null && unit.getType().name().equals("GROUP") ? unit.getUnitId() : null)
                .departmentId(unit != null && unit.getType().name().equals("DEPARTMENT") ? unit.getUnitId() : null)
                .description(a.getDescription())
                .startDate(a.getStartDate())
                .endDate(a.getEndDate())
                .registerDeadline(a.getRegisterDeadline())
                .registrationDeadline(a.getRegisterDeadline())
                .cancelDeadline(a.getCancelDeadline())
                .cancellationDeadline(a.getCancelDeadline())
                .location(a.getLocation())
                .isLimited(a.isLimited())
                .maxParticipants(a.getMaxParticipants())
                .createdBy(a.getCreatedBy())
                .managerUserId(a.getCreatedBy())
                .status(a.getStatus() != null ? a.getStatus().name().toLowerCase() : null)
                .createdDate(a.getCreatedDate())
                .build();
    }
}
