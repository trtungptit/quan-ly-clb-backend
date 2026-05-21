package com.example.clubmanagementbackend.domain.account.service;

import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.common.enums.UnitType;
import com.example.clubmanagementbackend.domain.account.dto.CreateAccountRequest;
import com.example.clubmanagementbackend.domain.account.dto.UpdateAccountRequest;
import com.example.clubmanagementbackend.domain.account.dto.UserAccountResponse;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
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
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final MemberRepository memberRepository;
    private final MemberUnitRepository memberUnitRepository;
    private final ClubUnitRepository clubUnitRepository;

    // ─── Helper: parse Role từ String lowercase/uppercase ───────────────────

    private Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role không được để trống");
        }
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role không hợp lệ: '" + roleStr + "'. Giá trị hợp lệ: member, group_leader, group_deputy, department_leader, department_deputy, club_leader, admin");
        }
    }

    private Status parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status không được để trống");
        }
        try {
            return Status.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status không hợp lệ: '" + statusStr + "'. Giá trị hợp lệ: active, inactive");
        }
    }

    /** Như parseStatus nhưng nếu null/blank thì trả về mặc định thay vì lỗi. */
    private Status parseStatusOrDefault(String statusStr, Status defaultStatus) {
        if (statusStr == null || statusStr.isBlank()) return defaultStatus;
        try {
            return Status.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status không hợp lệ: '" + statusStr + "'. Giá trị hợp lệ: active, inactive");
        }
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    public List<UserAccountResponse> getAllAccounts() {
        return userAccountRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserAccountResponse getAccountById(String userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
        return mapToResponse(account);
    }

    @Transactional
    public UserAccountResponse createAccount(CreateAccountRequest request) {
        // ── Từ chối nếu frontend vô tình gửi memberId ───────────────────────
        if (request.getMemberId() != null && !request.getMemberId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không được truyền memberId khi tạo tài khoản mới. Hệ thống sẽ tự tạo thành viên mới.");
        }

        // ── Kiểm tra fullName ────────────────────────────────────────────────
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập tên thành viên.");
        }

        // ── Kiểm tra username & password ────────────────────────────────────
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập username.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập password.");
        }
        if (userAccountRepository.existsByUsername(request.getUsername().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username đã tồn tại.");
        }

        // ── Parse role & status (status null → mặc định ACTIVE) ─────────────
        Role role = parseRole(request.getRole());
        Status status = parseStatusOrDefault(request.getStatus(), Status.ACTIVE);

        // ── Nếu là manager role thì bắt buộc managedUnitId ─────────────────
        boolean isManagerRole = role == Role.GROUP_LEADER || role == Role.GROUP_DEPUTY
                || role == Role.DEPARTMENT_LEADER || role == Role.DEPARTMENT_DEPUTY;
        if (isManagerRole && (request.getManagedUnitId() == null || request.getManagedUnitId().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "managedUnitId bắt buộc khi tạo tài khoản quản lý.");
        }

        // ── Kiểm tra club_leader trùng khi tạo mới ──────────────────────────
        if (role == Role.CLUB_LEADER && status == Status.ACTIVE) {
            long activeClubLeaders = userAccountRepository.findAll().stream()
                    .filter(a -> a.getRole() == Role.CLUB_LEADER && a.getStatus() == Status.ACTIVE)
                    .count();
            if (activeClubLeaders >= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Hệ thống đã có Trưởng CLB active.");
            }
        }

        // ── Tạo member mới (backend tự sinh memberId) ────────────────────────
        Member member = Member.builder()
                .memberId("m_" + UUID.randomUUID().toString().substring(0, 8))
                .fullName(request.getFullName().trim())
                .email(request.getEmail() != null ? request.getEmail() : "")
                .phone(request.getPhone() != null ? request.getPhone() : "")
                .gender(request.getGender() != null ? request.getGender() : "")
                .dateOfBirth(request.getDateOfBirth())
                .status(Status.ACTIVE)
                .build();
        member = memberRepository.save(member);

        // ── Tạo account gắn với member vừa tạo ──────────────────────────────
        UserAccount account = UserAccount.builder()
                .userId("u_" + UUID.randomUUID().toString().substring(0, 8))
                .member(member)
                .username(request.getUsername().trim())
                .password(request.getPassword())
                .role(role)
                .status(status)
                .build();

        account = userAccountRepository.save(account);

        // ── Tạo MemberUnit theo role + unit fields (Lỗi 1, 2, 3, 4) ─────────
        if (isManagerRole) {
            // Resolve unitId quản lý: ưu tiên managedUnitId, fallback groupUnitId/deptUnitId
            String primaryUnitId = null;
            if (request.getManagedUnitId() != null && !request.getManagedUnitId().isBlank()) {
                primaryUnitId = request.getManagedUnitId().trim();
            } else if ((role == Role.GROUP_LEADER || role == Role.GROUP_DEPUTY)
                    && request.getGroupUnitId() != null && !request.getGroupUnitId().isBlank()) {
                primaryUnitId = request.getGroupUnitId().trim();
            } else if ((role == Role.DEPARTMENT_LEADER || role == Role.DEPARTMENT_DEPUTY)
                    && request.getDepartmentUnitId() != null && !request.getDepartmentUnitId().isBlank()) {
                primaryUnitId = request.getDepartmentUnitId().trim();
            }

            if (primaryUnitId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "managedUnitId bắt buộc khi tạo tài khoản quản lý.");
            }

            final String finalPrimaryUnitId = primaryUnitId;
            ClubUnit primaryUnit = clubUnitRepository.findById(primaryUnitId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy đơn vị: " + finalPrimaryUnitId));

            // Validate unitType đúng với role
            UnitType expectedType = (role == Role.GROUP_LEADER || role == Role.GROUP_DEPUTY)
                    ? UnitType.GROUP : UnitType.DEPARTMENT;
            if (primaryUnit.getType() != expectedType) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Đơn vị phải là " + expectedType.name().toLowerCase()
                                + " cho role " + role.name().toLowerCase());
            }

            // Xác định position theo role
            Position pos = (role == Role.GROUP_LEADER || role == Role.DEPARTMENT_LEADER)
                    ? Position.LEADER : Position.DEPUTY;

            // Lỗi 3: check tất cả records (kể cả inactive) cùng unitType
            createMemberUnitIfAbsent(member.getMemberId(), primaryUnit, pos, status);

            // Nếu có unit phụ (department cho group_leader hoặc group cho dept_leader)
            String secondaryUnitId = null;
            if ((role == Role.GROUP_LEADER || role == Role.GROUP_DEPUTY)
                    && request.getDepartmentUnitId() != null && !request.getDepartmentUnitId().isBlank()) {
                secondaryUnitId = request.getDepartmentUnitId().trim();
            } else if ((role == Role.DEPARTMENT_LEADER || role == Role.DEPARTMENT_DEPUTY)
                    && request.getGroupUnitId() != null && !request.getGroupUnitId().isBlank()) {
                secondaryUnitId = request.getGroupUnitId().trim();
            }
            if (secondaryUnitId != null) {
                ClubUnit secondaryUnit = clubUnitRepository.findById(secondaryUnitId).orElse(null);
                if (secondaryUnit != null) {
                    createMemberUnitIfAbsent(member.getMemberId(), secondaryUnit, Position.MEMBER, status);
                }
            }

        } else if (role == Role.MEMBER) {
            // Member thường: tạo memberUnit cho groupUnitId và/hoặc departmentUnitId nếu có
            if (request.getManagedUnitId() != null && !request.getManagedUnitId().isBlank()) {
                // Nếu dùng managedUnitId thì xác định type từ unit
                ClubUnit unit = clubUnitRepository.findById(request.getManagedUnitId().trim()).orElse(null);
                if (unit != null) {
                    createMemberUnitIfAbsent(member.getMemberId(), unit, Position.MEMBER, status);
                }
            }
            if (request.getGroupUnitId() != null && !request.getGroupUnitId().isBlank()) {
                ClubUnit unit = clubUnitRepository.findById(request.getGroupUnitId().trim()).orElse(null);
                if (unit != null) {
                    createMemberUnitIfAbsent(member.getMemberId(), unit, Position.MEMBER, status);
                }
            }
            if (request.getDepartmentUnitId() != null && !request.getDepartmentUnitId().isBlank()) {
                ClubUnit unit = clubUnitRepository.findById(request.getDepartmentUnitId().trim()).orElse(null);
                if (unit != null) {
                    createMemberUnitIfAbsent(member.getMemberId(), unit, Position.MEMBER, status);
                }
            }
        }
        // CLUB_LEADER / ADMIN: không bắt buộc unit, nhưng nếu có groupUnitId/deptUnitId thì vẫn tạo
        else {
            if (request.getGroupUnitId() != null && !request.getGroupUnitId().isBlank()) {
                ClubUnit unit = clubUnitRepository.findById(request.getGroupUnitId().trim()).orElse(null);
                if (unit != null) createMemberUnitIfAbsent(member.getMemberId(), unit, Position.MEMBER, status);
            }
            if (request.getDepartmentUnitId() != null && !request.getDepartmentUnitId().isBlank()) {
                ClubUnit unit = clubUnitRepository.findById(request.getDepartmentUnitId().trim()).orElse(null);
                if (unit != null) createMemberUnitIfAbsent(member.getMemberId(), unit, Position.MEMBER, status);
            }
        }

        return mapToResponse(account);
    }

    /**
     * Tạo MemberUnit nếu:
     * 1. Chưa có bất kỳ record nào (kể cả inactive) cùng unitType cho member này.
     * 2. Chưa có record nào với unitId này cho member.
     * Nếu đã có thì throw lỗi nghiệp vụ.
     */
    private void createMemberUnitIfAbsent(String memberId, ClubUnit unit, Position position, Status status) {
        // Lỗi 3: check tất cả status, không chỉ ACTIVE
        List<MemberUnit> sameType = memberUnitRepository.findByMemberIdAndUnitType(memberId, unit.getType());
        if (!sameType.isEmpty()) {
            // Nếu đã có record cho đúng unit này thì bỏ qua (không ném lỗi)
            boolean sameUnit = sameType.stream().anyMatch(mu -> mu.getUnitId().equals(unit.getUnitId()));
            if (!sameUnit) {
                String typeLabel = unit.getType() == UnitType.GROUP ? "nhóm" : "ban";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Thành viên đã thuộc một " + typeLabel + " khác (kể cả inactive).");
            }
            return; // đã có record với unit này, skip
        }
        if (!memberUnitRepository.existsByMemberIdAndUnitId(memberId, unit.getUnitId())) {
            memberUnitRepository.save(MemberUnit.builder()
                    .id(UUID.randomUUID().toString())
                    .memberId(memberId)
                    .unitId(unit.getUnitId())
                    .position(position)
                    .status(status)
                    .build());
        }
    }


    @Transactional
    public UserAccountResponse updateAccount(String userId, UpdateAccountRequest request, String currentUserId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));

        if (request.getUsername() != null && !request.getUsername().equals(account.getUsername())) {
            if (userAccountRepository.existsByUsername(request.getUsername())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Username '" + request.getUsername() + "' đã tồn tại");
            }
            account.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            account.setPassword(request.getPassword());
        }

        // Parse role/status nếu được gửi lên
        Role newRoleParsed  = (request.getRole()   != null) ? parseRole(request.getRole())     : null;
        Status newStatusParsed = (request.getStatus() != null) ? parseStatus(request.getStatus()) : null;

        if (userId.equals(currentUserId)) {
            if (newStatusParsed != null && newStatusParsed != account.getStatus()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể tự thay đổi trạng thái tài khoản của mình");
            }
            if (newRoleParsed != null && newRoleParsed != account.getRole()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể tự thay đổi vai trò của mình");
            }
        } else {
            if (newRoleParsed != null || newStatusParsed != null || request.getManagedUnitId() != null) {
                Role effectiveRole   = newRoleParsed   != null ? newRoleParsed   : account.getRole();
                Status effectiveStatus = newStatusParsed != null ? newStatusParsed : account.getStatus();

                checkAndApplyManagerRole(effectiveRole, effectiveStatus, account.getMember().getMemberId(), request.getManagedUnitId(), userId);

                if (newRoleParsed   != null) account.setRole(newRoleParsed);
                if (newStatusParsed != null) account.setStatus(newStatusParsed);
            }
        }

        return mapToResponse(userAccountRepository.save(account));
    }

    // ─── Business logic: kiểm tra & gán manager role ─────────────────────────

    private void checkAndApplyManagerRole(Role role, Status status, String memberId, String managedUnitId, String excludeUserId) {
        if (role == Role.CLUB_LEADER && status == Status.ACTIVE) {
            long activeClubLeaders = userAccountRepository.findAll().stream()
                    .filter(a -> a.getRole() == Role.CLUB_LEADER && a.getStatus() == Status.ACTIVE
                            && (excludeUserId == null || !a.getUserId().equals(excludeUserId)))
                    .count();
            if (activeClubLeaders >= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Câu lạc bộ đã có một chủ nhiệm đang hoạt động");
            }
        }

        boolean isManagerRole = role == Role.GROUP_LEADER || role == Role.GROUP_DEPUTY
                || role == Role.DEPARTMENT_LEADER || role == Role.DEPARTMENT_DEPUTY;

        if (isManagerRole && status == Status.ACTIVE) {
            if (managedUnitId == null || managedUnitId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "managedUnitId bắt buộc khi gán vai trò quản lý");
            }

            ClubUnit unit = clubUnitRepository.findById(managedUnitId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy đơn vị với ID: " + managedUnitId));

            if ((role == Role.GROUP_LEADER || role == Role.GROUP_DEPUTY)
                    && !unit.getType().name().equals("GROUP")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "managedUnitId phải là nhóm (GROUP) khi gán vai trò trưởng/phó nhóm");
            }

            if ((role == Role.DEPARTMENT_LEADER || role == Role.DEPARTMENT_DEPUTY)
                    && !unit.getType().name().equals("DEPARTMENT")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "managedUnitId phải là ban (DEPARTMENT) khi gán vai trò trưởng/phó ban");
            }

            MemberUnit mu = memberUnitRepository.findByMemberIdAndUnitId(memberId, managedUnitId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Thành viên không thuộc đơn vị này"));

            if (mu.getStatus() != Status.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Thành viên phải có trạng thái ACTIVE trong đơn vị này");
            }

            if (role == Role.GROUP_LEADER || role == Role.DEPARTMENT_LEADER) {
                String roleLabel = (role == Role.GROUP_LEADER) ? "nhóm" : "ban";
                long activeLeaders = userAccountRepository.findAll().stream()
                        .filter(a -> a.getRole() == role && a.getStatus() == Status.ACTIVE
                                && (excludeUserId == null || !a.getUserId().equals(excludeUserId)))
                        .filter(a -> memberUnitRepository.findByMemberIdAndUnitId(
                                a.getMember().getMemberId(), managedUnitId)
                                .map(m -> m.getStatus() == Status.ACTIVE && m.getPosition() == Position.LEADER)
                                .orElse(false))
                        .count();
                if (activeLeaders >= 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Đơn vị này đã có trưởng " + roleLabel + " đang hoạt động");
                }
            }

            // Hạ cấp position ở các đơn vị khác về MEMBER
            List<MemberUnit> allUnits = memberUnitRepository.findByMemberId(memberId);
            for (MemberUnit existingMu : allUnits) {
                if (!existingMu.getUnitId().equals(managedUnitId)
                        && (existingMu.getPosition() == Position.LEADER || existingMu.getPosition() == Position.DEPUTY)) {
                    existingMu.setPosition(Position.MEMBER);
                    memberUnitRepository.save(existingMu);
                }
            }

            // Gán position trong managedUnitId
            Position newPos = (role == Role.GROUP_LEADER || role == Role.DEPARTMENT_LEADER)
                    ? Position.LEADER : Position.DEPUTY;
            mu.setPosition(newPos);
            memberUnitRepository.save(mu);

        } else {
            // Không phải manager role → hạ tất cả position về MEMBER
            List<MemberUnit> allUnits = memberUnitRepository.findByMemberId(memberId);
            for (MemberUnit existingMu : allUnits) {
                if (existingMu.getPosition() == Position.LEADER || existingMu.getPosition() == Position.DEPUTY) {
                    existingMu.setPosition(Position.MEMBER);
                    memberUnitRepository.save(existingMu);
                }
            }
        }
    }

    // ─── Map entity → response ────────────────────────────────────────────────

    private UserAccountResponse mapToResponse(UserAccount account) {
        String managedUnitId = null;
        if (account.getMember() != null && account.getRole() != null) {
            boolean isManagerRole = account.getRole() == Role.GROUP_LEADER
                    || account.getRole() == Role.GROUP_DEPUTY
                    || account.getRole() == Role.DEPARTMENT_LEADER
                    || account.getRole() == Role.DEPARTMENT_DEPUTY;
            if (isManagerRole && account.getStatus() == Status.ACTIVE) {
                managedUnitId = memberUnitRepository.findByMemberId(account.getMember().getMemberId()).stream()
                        .filter(mu -> mu.getStatus() == Status.ACTIVE
                                && (mu.getPosition() == Position.LEADER || mu.getPosition() == Position.DEPUTY))
                        .map(MemberUnit::getUnitId)
                        .findFirst()
                        .orElse(null);
            }
        }

        return UserAccountResponse.builder()
                .userId(account.getUserId())
                .memberId(account.getMember() != null ? account.getMember().getMemberId() : null)
                .username(account.getUsername())
                .role(account.getRole() != null ? account.getRole().name().toLowerCase() : null)
                .status(account.getStatus() != null ? account.getStatus().name().toLowerCase() : null)
                .managedUnitId(managedUnitId)
                .fullName(account.getMember() != null ? account.getMember().getFullName() : null)
                .email(account.getMember() != null ? account.getMember().getEmail() : null)
                .phone(account.getMember() != null ? account.getMember().getPhone() : null)
                .gender(account.getMember() != null ? account.getMember().getGender() : null)
                .dateOfBirth(account.getMember() != null ? account.getMember().getDateOfBirth() : null)
                .build();
    }
}
