package com.example.clubmanagementbackend.domain.annualprogram.repository;

import com.example.clubmanagementbackend.domain.annualprogram.entity.AnnualProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnualProgramRepository extends JpaRepository<AnnualProgram, String> {
    List<AnnualProgram> findByDeletedFalse();
}
