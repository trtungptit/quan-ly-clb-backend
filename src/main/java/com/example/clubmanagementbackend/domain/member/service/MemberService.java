package com.example.clubmanagementbackend.domain.member.service;

import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.group.dto.ManagedMemberResponse;
import com.example.clubmanagementbackend.domain.group.dto.MemberUnitResponse;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.dto.MemberResponse;
import com.example.clubmanagementbackend.domain.member.dto.UpdateMemberRequest;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final MemberUnitRepository memberUnitRepository;
    private final ClubUnitRepository clubUnitRepository;

    public List<MemberResponse> getAllMembers() {
        return memberRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MemberResponse getMemberById(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên"));

        return toResponse(member);
    }

    public MemberResponse updateMember(String memberId, UpdateMemberRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên"));

        member.setFullName(request.getFullName());
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());
        member.setGender(request.getGender());
        member.setDateOfBirth(request.getDateOfBirth());

        Member saved = memberRepository.save(member);
        return toResponse(saved);
    }

    /**
     * Trả danh sách member thuộc unit mà manager đang quản lý.
     * - CLUB_LEADER / ADMIN → toàn bộ members
     * - GROUP_LEADER/DEPUTY, DEPARTMENT_LEADER/DEPUTY → members thuộc unit mình quản lý
     * - Vai trò khác (MEMBER) → 403
     */
    public List<ManagedMemberResponse> getManagedMembers(String managerUserId) {
        // 1. Validate input
        if (managerUserId == null || managerUserId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "managerUserId is required.");
        }

        // 2. Tìm account
        UserAccount account = userAccountRepository.findById(managerUserId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found."));

        Role role = account.getRole();

        // 3. CLUB_LEADER hoặc ADMIN → trả tất cả members (bao gồm cả inactive)
        if (role == Role.CLUB_LEADER || role == Role.ADMIN) {
            // Lỗi 5: Không distinct theo memberId — trả từng MemberUnit riêng
            // (1 member có thể có 1 record nhóm + 1 record ban)
            return memberUnitRepository.findAll().stream()
                    .map(mu -> {
                        Member m = memberRepository.findById(mu.getMemberId()).orElse(null);
                        if (m == null) return null;
                        ClubUnit unit = clubUnitRepository.findById(mu.getUnitId()).orElse(null);
                        return toManagedResponse(m, mu, unit);
                    })
                    .filter(r -> r != null)
                    .collect(Collectors.toList());
        }

        // 4. Chỉ các manager role mới được dùng endpoint này
        boolean isManagerRole = role == Role.GROUP_LEADER || role == Role.GROUP_DEPUTY
                || role == Role.DEPARTMENT_LEADER || role == Role.DEPARTMENT_DEPUTY;
        if (!isManagerRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only managers can view managed members.");
        }

        // 5. Lấy memberId của manager
        if (account.getMember() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Account has no linked member.");
        }
        String managerMemberId = account.getMember().getMemberId();

        // 6. Tìm các unitId manager đang quản lý (ACTIVE + LEADER/DEPUTY)
        List<String> managedUnitIds = memberUnitRepository.findByMemberId(managerMemberId)
                .stream()
                .filter(mu -> mu.getStatus() == Status.ACTIVE
                        && (mu.getPosition() == Position.LEADER || mu.getPosition() == Position.DEPUTY))
                .map(MemberUnit::getUnitId)
                .collect(Collectors.toList());

        // 7. Nếu không quản lý unit nào → trả []
        if (managedUnitIds.isEmpty()) {
            return List.of();
        }

        // 8. Lấy TẤT CẢ MemberUnit thuộc các unit đó (cả active và inactive)
        // Lỗi 6: không distinct theo memberId — 1 member có thể có cả nhóm lẫn ban
        return memberUnitRepository.findByUnitIdIn(managedUnitIds)
                .stream()
                .map(mu -> {
                    Member m = memberRepository.findById(mu.getMemberId()).orElse(null);
                    if (m == null) return null;
                    ClubUnit unit = clubUnitRepository.findById(mu.getUnitId()).orElse(null);
                    return toManagedResponse(m, mu, unit);
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }


    /**
     * Trả các nhóm/ban thật mà một thành viên đang thuộc về từ bảng member_units.
     * Dùng cho frontend lọc hoạt động theo đúng nhóm/ban, không phụ thuộc vào đơn đăng ký cũ.
     */
    public List<MemberUnitResponse> getMemberUnits(String memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found: " + memberId);
        }
        return memberUnitRepository.findByMemberId(memberId)
                .stream()
                .filter(mu -> mu.getStatus() == Status.ACTIVE)
                .map(mu -> {
                    Member m = memberRepository.findById(mu.getMemberId()).orElse(null);
                    ClubUnit unit = clubUnitRepository.findById(mu.getUnitId()).orElse(null);
                    return MemberUnitResponse.builder()
                            .memberUnitId(mu.getId())
                            .memberId(mu.getMemberId())
                            .memberName(m != null ? m.getFullName() : null)
                            .unitId(mu.getUnitId())
                            .unitName(unit != null ? unit.getUnitName() : null)
                            .unitType(unit != null && unit.getType() != null ? unit.getType().name().toLowerCase() : null)
                            .position(mu.getPosition() != null ? mu.getPosition().name().toLowerCase() : null)
                            .status(mu.getStatus() != null ? mu.getStatus().name().toLowerCase() : null)
                            .accountStatus(userAccountRepository.findByMember_MemberId(mu.getMemberId())
                                    .map(acc -> acc.getStatus() != null ? acc.getStatus().name().toLowerCase() : null)
                                    .orElse(null))
                            .build();
                })
                .toList();
    }

    private MemberResponse toResponse(Member member) {
        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .gender(member.getGender())
                .dateOfBirth(member.getDateOfBirth())
                .status(member.getStatus() != null ? member.getStatus().name().toLowerCase() : null)
                .build();
    }

    private ManagedMemberResponse toManagedResponse(Member member, MemberUnit mu, ClubUnit unit) {
        String accountStatus = userAccountRepository.findByMember_MemberId(member.getMemberId())
                .map(acc -> acc.getStatus() != null ? acc.getStatus().name().toLowerCase() : null)
                .orElse(null);
        return ManagedMemberResponse.builder()
                .memberId(member.getMemberId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .gender(member.getGender())
                .dateOfBirth(member.getDateOfBirth())
                .status(member.getStatus() != null ? member.getStatus().name().toLowerCase() : null)
                // member_unit context
                .memberUnitId(mu != null ? mu.getId() : null)
                .unitId(mu != null ? mu.getUnitId() : null)
                .unitName(unit != null ? unit.getUnitName() : null)
                .unitType(unit != null && unit.getType() != null ? unit.getType().name().toLowerCase() : null)
                .position(mu != null && mu.getPosition() != null ? mu.getPosition().name().toLowerCase() : null)
                .memberUnitStatus(mu != null && mu.getStatus() != null ? mu.getStatus().name().toLowerCase() : null)
                .accountStatus(accountStatus)
                .build();
    }
}
