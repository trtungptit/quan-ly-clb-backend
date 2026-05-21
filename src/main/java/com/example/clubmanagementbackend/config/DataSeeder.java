package com.example.clubmanagementbackend.config;

import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.Role;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.common.enums.UnitType;
import com.example.clubmanagementbackend.domain.account.entity.UserAccount;
import com.example.clubmanagementbackend.domain.account.repository.UserAccountRepository;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import com.example.clubmanagementbackend.domain.group.repository.ClubUnitRepository;
import com.example.clubmanagementbackend.domain.group.repository.MemberUnitRepository;
import com.example.clubmanagementbackend.domain.member.entity.Member;
import com.example.clubmanagementbackend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClubUnitRepository clubUnitRepository;
    private final MemberUnitRepository memberUnitRepository;

    @Override
    public void run(String... args) {

        // ─── Units ─────────────────────────────────────────────────────────────
        seedUnit("g_01", "Nhóm Nhi Trung Ương",          UnitType.GROUP);
        seedUnit("g_02", "Nhóm Huyết Học",               UnitType.GROUP);
        seedUnit("g_03", "Nhóm K3 Tân Triều",            UnitType.GROUP);
        seedUnit("d_01", "Ban Truyền thông sự kiện",      UnitType.DEPARTMENT);
        seedUnit("d_02", "Ban Hậu cần",                   UnitType.DEPARTMENT);
        seedUnit("d_03", "Ban Văn nghệ",                  UnitType.DEPARTMENT);

        // ─── Members ───────────────────────────────────────────────────────────
        // Club leader
        Member club = seedMember("m_club", "Chủ Tịch CLB",        "chutich@clb.vn",    "0900000001", "Nam",  null,                      Status.ACTIVE);
        // Group leader (g_01)
        Member gl   = seedMember("m_gl",   "Trần Trưởng Nhóm Nhi","gl@clb.vn",         "0900000002", "Nữ",   LocalDate.of(1995, 3, 15), Status.ACTIVE);
        // Department leader (d_01)
        Member dl   = seedMember("m_dl",   "Nguyễn Trưởng Ban TT","dl@clb.vn",         "0900000003", "Nam",  LocalDate.of(1996, 7, 20), Status.ACTIVE);
        // Regular member — thuộc cả g_01 (nhóm) lẫn d_02 (ban)
        Member mem  = seedMember("m_mem",  "Nguyễn Thành Viên",   "member@clb.vn",     "0900000004", "Nam",  LocalDate.of(2000, 1, 10), Status.ACTIVE);
        // Inactive member — để test "inactive vẫn hiển thị"
        Member inac = seedMember("m_inac", "Lê Ngừng Hoạt Động",  "inactive@clb.vn",   "0900000005", "Nữ",   LocalDate.of(1998, 5, 5),  Status.ACTIVE);

        // ─── Accounts ──────────────────────────────────────────────────────────
        seedAccount("u_club", club, "clubleader",  "123", Role.CLUB_LEADER,       Status.ACTIVE);
        seedAccount("u_gl",   gl,   "groupleader", "123", Role.GROUP_LEADER,      Status.ACTIVE);
        seedAccount("u_dl",   dl,   "deptleader",  "123", Role.DEPARTMENT_LEADER, Status.ACTIVE);
        seedAccount("u_mem",  mem,  "member",      "123", Role.MEMBER,            Status.ACTIVE);
        seedAccount("u_inac", inac, "inactive",    "123", Role.MEMBER,            Status.INACTIVE);

        // ─── MemberUnits ───────────────────────────────────────────────────────
        // Group leader → g_01 (LEADER)
        seedMemberUnit("m_gl",   "g_01", Position.LEADER, Status.ACTIVE);
        // Department leader → d_01 (LEADER)
        seedMemberUnit("m_dl",   "d_01", Position.LEADER, Status.ACTIVE);
        // Regular member → g_01 (MEMBER) + d_02 (MEMBER)
        seedMemberUnit("m_mem",  "g_01", Position.MEMBER, Status.ACTIVE);
        seedMemberUnit("m_mem",  "d_02", Position.MEMBER, Status.ACTIVE);
        // Inactive member → g_02 nhưng INACTIVE
        seedMemberUnit("m_inac", "g_02", Position.MEMBER, Status.INACTIVE);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void seedUnit(String id, String name, UnitType type) {
        if (!clubUnitRepository.existsById(id)) {
            clubUnitRepository.save(ClubUnit.builder()
                    .unitId(id)
                    .unitName(name)
                    .type(type)
                    .description(name)
                    .status(Status.ACTIVE)
                    .build());
        }
    }

    private Member seedMember(String id, String fullName, String email, String phone,
                               String gender, LocalDate dob, Status status) {
        return memberRepository.findById(id).orElseGet(() ->
                memberRepository.save(Member.builder()
                        .memberId(id)
                        .fullName(fullName)
                        .email(email)
                        .phone(phone)
                        .gender(gender)
                        .dateOfBirth(dob)
                        .status(status)
                        .build()));
    }

    private void seedAccount(String userId, Member member, String username,
                              String password, Role role, Status status) {
        if (!userAccountRepository.existsByUsername(username)) {
            userAccountRepository.save(UserAccount.builder()
                    .userId(userId)
                    .member(member)
                    .username(username)
                    .password(password)
                    .role(role)
                    .status(status)
                    .build());
        }
    }

    private void seedMemberUnit(String memberId, String unitId, Position position, Status status) {
        if (!memberUnitRepository.existsByMemberIdAndUnitId(memberId, unitId)) {
            memberUnitRepository.save(MemberUnit.builder()
                    .id(UUID.randomUUID().toString())
                    .memberId(memberId)
                    .unitId(unitId)
                    .position(position)
                    .status(status)
                    .build());
        }
    }
}
