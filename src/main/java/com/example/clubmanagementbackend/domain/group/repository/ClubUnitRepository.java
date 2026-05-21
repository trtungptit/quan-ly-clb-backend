package com.example.clubmanagementbackend.domain.group.repository;
import com.example.clubmanagementbackend.domain.group.entity.ClubUnit;
import com.example.clubmanagementbackend.common.enums.UnitType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ClubUnitRepository extends JpaRepository<ClubUnit, String> {
    List<ClubUnit> findByType(UnitType type);
}
