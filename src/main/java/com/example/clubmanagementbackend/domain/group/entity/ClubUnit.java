package com.example.clubmanagementbackend.domain.group.entity;
import com.example.clubmanagementbackend.common.enums.Status;
import com.example.clubmanagementbackend.common.enums.UnitType;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "club_units")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClubUnit {
    @Id @Column(length = 50) private String unitId;
    @Column(nullable = false) private String unitName;
    @Enumerated(EnumType.STRING) private UnitType type;
    @Column(length = 1000) private String description;
    @Enumerated(EnumType.STRING) private Status status;
}
