package com.example.clubmanagementbackend.domain.activity.repository;

import com.example.clubmanagementbackend.domain.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, String> {
    List<Activity> findByUnitId(String unitId);
    List<Activity> findByUnitIdIn(List<String> unitIds);
}

