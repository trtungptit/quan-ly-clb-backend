package com.example.clubmanagementbackend.domain.group.entity;
import com.example.clubmanagementbackend.common.enums.Position;
import com.example.clubmanagementbackend.common.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "member_units")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberUnit {
    @Id @Column(length = 50) private String id;
    @Column(length = 50, nullable = false) private String unitId;
    @Column(length = 50, nullable = false) private String memberId;
    @Enumerated(EnumType.STRING) private Position position;
    @Enumerated(EnumType.STRING) private Status status;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

