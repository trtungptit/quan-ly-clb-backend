package com.example.clubmanagementbackend.domain.group.repository;
import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.common.enums.UnitType;
import com.example.clubmanagementbackend.domain.group.entity.MemberUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MemberUnitRepository extends JpaRepository<MemberUnit, String> {
    List<MemberUnit> findByMemberId(String memberId);
    List<MemberUnit> findByUnitId(String unitId);
    boolean existsByMemberIdAndUnitId(String memberId, String unitId);
    java.util.Optional<MemberUnit> findByMemberIdAndUnitId(String memberId, String unitId);

    // Lấy MemberUnit theo danh sách unitId + filter status
    List<MemberUnit> findByUnitIdInAndStatus(List<String> unitIds, Status status);

    // Lấy TẤT CẢ MemberUnit trong danh sách unitId (cả active và inactive)
    List<MemberUnit> findByUnitIdIn(List<String> unitIds);

    // Lỗi 3: kiểm tra ANY record (kể cả inactive) để enforce 1 GROUP + 1 DEPARTMENT per member
    // Dùng khi assign / createAccount để ngăn tạo unit thứ 2 cùng type
    @Query("SELECT mu FROM MemberUnit mu JOIN ClubUnit cu ON mu.unitId = cu.unitId " +
           "WHERE mu.memberId = :memberId AND cu.type = :unitType")
    List<MemberUnit> findByMemberIdAndUnitType(
            @Param("memberId") String memberId,
            @Param("unitType") UnitType unitType);

    // Chỉ lấy ACTIVE records — dùng cho recalculate role
    @Query("SELECT mu FROM MemberUnit mu JOIN ClubUnit cu ON mu.unitId = cu.unitId " +
           "WHERE mu.memberId = :memberId AND cu.type = :unitType AND mu.status = 'ACTIVE'")
    List<MemberUnit> findActiveByMemberIdAndUnitType(
            @Param("memberId") String memberId,
            @Param("unitType") UnitType unitType);

    // Tìm leader active khác trong cùng unit (loại trừ chính record đang sửa)
    // Dùng để check không cho 2 leader cùng unit
    @Query("SELECT mu FROM MemberUnit mu WHERE mu.unitId = :unitId " +
           "AND mu.position = :leaderPos AND mu.status = :activeStatus " +
           "AND mu.id <> :excludeId")
    List<MemberUnit> findOtherActiveLeadersInUnit(
            @Param("unitId") String unitId,
            @Param("leaderPos") Position leaderPos,
            @Param("activeStatus") Status activeStatus,
            @Param("excludeId") String excludeId);
}
