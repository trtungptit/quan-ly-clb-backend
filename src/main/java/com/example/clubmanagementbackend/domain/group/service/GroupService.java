package com.example.clubmanagementbackend.domain.group.service;

import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.common.enums.UnitType;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.group.dto.AssignMemberRequest;
import com.example.clubmanagementbackend.domain.group.dto.ClubUnitResponse;
import com.example.clubmanagementbackend.domain.group.dto.MemberUnitResponse;
import com.example.clubmanagementbackend.domain.group.dto.UpdateMemberUnitRequest;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
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
public class GroupService {
    private final ClubUnitRepository clubUnitRepository;
    private final MemberUnitRepository memberUnitRepository;
    private final UserAccountRepository userAccountRepository;
    private final MemberRepository memberRepository;

    // ─── Read operations ──────────────────────────────────────────────────────

    public List<ClubUnitResponse> getAllUnits() {
        return clubUnitRepository.findAll().stream().map(this::mapToUnitResponse).collect(Collectors.toList());
    }
    public List<ClubUnitResponse> getGroups() {
        return clubUnitRepository.findByType(UnitType.GROUP).stream().map(this::mapToUnitResponse).collect(Collectors.toList());
    }
    public List<ClubUnitResponse> getDepartments() {
        return clubUnitRepository.findByType(UnitType.DEPARTMENT).stream().map(this::mapToUnitResponse).collect(Collectors.toList());
    }

    // ─── Assign member to unit ─────────────────────────────────────────────────
    // Lỗi 4: thêm check unitType trùng; Lỗi 5: bỏ rule cấm leader ở cả group+dept

    @Transactional
    public MemberUnitResponse assignMember(String unitId, AssignMemberRequest request) {
        // Tìm unit
        ClubUnit unit = clubUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + unitId));

        // Không cho assign lại đúng unit đó
        if (memberUnitRepository.existsByMemberIdAndUnitId(request.getMemberId(), unitId)) {
            throw new RuntimeException("Member already in this unit");
        }

        // Lỗi 4: kiểm tra member đã có bất kỳ record nào (kể cả inactive) của cùng UnitType
        // Vì nghiệp vụ: 1 member chỉ có 1 GROUP record và 1 DEPARTMENT record tất cả thời gian
        List<MemberUnit> sameType = memberUnitRepository.findByMemberIdAndUnitType(
                request.getMemberId(), unit.getType());
        if (!sameType.isEmpty()) {
            String typeLabel = unit.getType() == UnitType.GROUP ? "nhóm" : "ban";
            throw new RuntimeException("Thành viên đã thuộc một " + typeLabel
                    + " khác (kể cả inactive). Không thể thêm " + typeLabel + " thứ 2.");
        }

        Position pos = Position.valueOf(request.getPosition().toUpperCase());

        // Kiểm tra unit đã có leader active chưa (nếu assign leader)
        if (pos == Position.LEADER) {
            long activeLeaders = memberUnitRepository.findByUnitId(unitId).stream()
                    .filter(mu -> mu.getStatus() == Status.ACTIVE && mu.getPosition() == Position.LEADER)
                    .count();
            if (activeLeaders >= 1) {
                throw new RuntimeException("This unit already has an active leader");
            }
        }

        // Lỗi 5: BỎ rule cấm leader ở cả group + department. Nghiệp vụ không cấm.

        MemberUnit mu = MemberUnit.builder()
                .id(UUID.randomUUID().toString())
                .unitId(unitId)
                .memberId(request.getMemberId())
                .position(pos)
                .status(Status.ACTIVE)
                .build();
        MemberUnit saved = memberUnitRepository.save(mu);

        // Recalculate account role sau khi assign
        recalculateAccountRole(request.getMemberId());

        return mapToMemberUnitResponse(saved);
    }

    // ─── Update member position / status within managed unit ─────────────────

    @Transactional
    public MemberUnitResponse updateMemberUnit(
            String memberUnitId,
            String managerUserId,
            UpdateMemberUnitRequest request) {

        // 1. Validate managerUserId
        if (managerUserId == null || managerUserId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "managerUserId is required.");
        }

        // 2. Tìm manager account
        UserAccount managerAccount = userAccountRepository.findById(managerUserId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager account not found."));

        Role managerRole = managerAccount.getRole();
        boolean isClubLeader = (managerRole == Role.CLUB_LEADER || managerRole == Role.ADMIN);
        boolean isManagerRole = isClubLeader
                || managerRole == Role.GROUP_LEADER || managerRole == Role.GROUP_DEPUTY
                || managerRole == Role.DEPARTMENT_LEADER || managerRole == Role.DEPARTMENT_DEPUTY;

        // 3. Kiểm tra role
        if (!isManagerRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only managers can update member units.");
        }

        // 4. Tìm MemberUnit target
        MemberUnit target = memberUnitRepository.findById(memberUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member unit not found."));

        // 5. Kiểm tra quyền theo unit
        if (!isClubLeader) {
            if (managerAccount.getMember() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager has no linked member.");
            }
            String managerMemberId = managerAccount.getMember().getMemberId();
            boolean hasAuthority = memberUnitRepository
                    .findByMemberIdAndUnitId(managerMemberId, target.getUnitId())
                    .map(mu -> mu.getStatus() == Status.ACTIVE
                            && (mu.getPosition() == Position.LEADER || mu.getPosition() == Position.DEPUTY))
                    .orElse(false);
            if (!hasAuthority) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not manage this unit.");
            }
        }

        // 6. Bảo vệ self-demotion
        if (managerAccount.getMember() != null) {
            String managerMemberId = managerAccount.getMember().getMemberId();
            if (managerMemberId.equals(target.getMemberId())) {
                boolean demotingStatus = request.getStatus() != null
                        && request.getStatus().trim().equalsIgnoreCase("inactive");
                boolean demotingPosition = request.getPosition() != null
                        && request.getPosition().trim().equalsIgnoreCase("member")
                        && (target.getPosition() == Position.LEADER || target.getPosition() == Position.DEPUTY);
                if (demotingStatus || demotingPosition) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "You cannot remove your own manager permission.");
                }
            }
        }

        // 7. Parse và set status nếu có → đồng bộ account.status
        boolean statusChanged = false;
        Status newStatusValue = null;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                newStatusValue = Status.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status. Allowed: active, inactive.");
            }
            if (newStatusValue != Status.ACTIVE && newStatusValue != Status.INACTIVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status. Allowed: active, inactive.");
            }
            target.setStatus(newStatusValue);
            statusChanged = true;

            // Đồng bộ user_accounts.status theo memberUnit.status
            final Status finalStatus = newStatusValue;
            userAccountRepository.findByMember_MemberId(target.getMemberId())
                    .ifPresent(acc -> {
                        acc.setStatus(finalStatus);
                        userAccountRepository.save(acc);
                    });
        }

        // 8. Parse và set position nếu có
        boolean positionChanged = false;
        if (request.getPosition() != null && !request.getPosition().trim().isEmpty()) {
            Position newPosition;
            try {
                newPosition = Position.valueOf(request.getPosition().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid position. Allowed: member, deputy" + (isClubLeader ? ", leader." : "."));
            }

            // Manager thường không được set LEADER
            if (!isClubLeader && newPosition == Position.LEADER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only club leader can assign leader position.");
            }

            // Lỗi 6: Kiểm tra unit đã có leader active khác chưa
            if (newPosition == Position.LEADER) {
                List<MemberUnit> otherLeaders = memberUnitRepository.findOtherActiveLeadersInUnit(
                        target.getUnitId(), Position.LEADER, Status.ACTIVE, target.getId());
                if (!otherLeaders.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Đơn vị này đã có trưởng nhóm/ban active. Không thể thêm trưởng thứ 2.");
                }
            }

            target.setPosition(newPosition);
            positionChanged = true;
        }

        // 9. Lưu
        memberUnitRepository.save(target);

        // 10. Lỗi 7: recalculate role dựa trên TẤT CẢ member_units active của member
        // (không chỉ dựa vào record vừa sửa)
        if (positionChanged || statusChanged) {
            recalculateAccountRole(target.getMemberId());
        }

        return mapToMemberUnitResponse(target);
    }

    // ─── Helper: tính lại role account từ toàn bộ member_units active ────────

    /**
     * Lỗi 7 fix: Không set role theo 1 record đang sửa.
     * Thay vào đó tính lại role theo ưu tiên từ tất cả MemberUnit ACTIVE của member.
     *
     * Ưu tiên:
     * 1. GROUP + LEADER    => GROUP_LEADER
     * 2. DEPARTMENT + LEADER => DEPARTMENT_LEADER
     * 3. GROUP + DEPUTY    => GROUP_DEPUTY
     * 4. DEPARTMENT + DEPUTY => DEPARTMENT_DEPUTY
     * 5. Không có gì       => MEMBER
     */
    void recalculateAccountRole(String memberId) {
        userAccountRepository.findByMember_MemberId(memberId).ifPresent(acc -> {
            List<MemberUnit> activeMus = memberUnitRepository.findByMemberId(memberId)
                    .stream()
                    .filter(mu -> mu.getStatus() == Status.ACTIVE)
                    .collect(Collectors.toList());

            Role newRole = Role.MEMBER; // default

            boolean groupLeader = false, deptLeader = false;
            boolean groupDeputy = false, deptDeputy = false;

            for (MemberUnit mu : activeMus) {
                ClubUnit unit = clubUnitRepository.findById(mu.getUnitId()).orElse(null);
                if (unit == null) continue;
                if (unit.getType() == UnitType.GROUP) {
                    if (mu.getPosition() == Position.LEADER) groupLeader = true;
                    else if (mu.getPosition() == Position.DEPUTY) groupDeputy = true;
                } else if (unit.getType() == UnitType.DEPARTMENT) {
                    if (mu.getPosition() == Position.LEADER) deptLeader = true;
                    else if (mu.getPosition() == Position.DEPUTY) deptDeputy = true;
                }
            }

            if (groupLeader)       newRole = Role.GROUP_LEADER;
            else if (deptLeader)   newRole = Role.DEPARTMENT_LEADER;
            else if (groupDeputy)  newRole = Role.GROUP_DEPUTY;
            else if (deptDeputy)   newRole = Role.DEPARTMENT_DEPUTY;

            acc.setRole(newRole);
            userAccountRepository.save(acc);
        });
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private ClubUnitResponse mapToUnitResponse(ClubUnit unit) {
        return ClubUnitResponse.builder()
                .unitId(unit.getUnitId())
                .unitName(unit.getUnitName())
                .type(unit.getType().name().toLowerCase())
                .description(unit.getDescription())
                .status(unit.getStatus().name().toLowerCase())
                .build();
    }

    private MemberUnitResponse mapToMemberUnitResponse(MemberUnit mu) {
        String memberName = memberRepository.findById(mu.getMemberId())
                .map(Member::getFullName).orElse(null);
        ClubUnit unit = clubUnitRepository.findById(mu.getUnitId()).orElse(null);
        String accountStatus = userAccountRepository.findByMember_MemberId(mu.getMemberId())
                .map(acc -> acc.getStatus() != null ? acc.getStatus().name().toLowerCase() : null)
                .orElse(null);

        return MemberUnitResponse.builder()
                .memberUnitId(mu.getId())
                .memberId(mu.getMemberId())
                .memberName(memberName)
                .unitId(mu.getUnitId())
                .unitName(unit != null ? unit.getUnitName() : null)
                .unitType(unit != null && unit.getType() != null ? unit.getType().name().toLowerCase() : null)
                .position(mu.getPosition() != null ? mu.getPosition().name().toLowerCase() : null)
                .status(mu.getStatus() != null ? mu.getStatus().name().toLowerCase() : null)
                .accountStatus(accountStatus)
                .build();
    }
}
