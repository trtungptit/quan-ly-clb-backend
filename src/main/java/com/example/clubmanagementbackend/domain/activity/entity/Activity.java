package com.example.clubmanagementbackend.domain.activity.entity;
import com.example.clubmanagementbackend.common.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity
@Table(name = "activities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Activity {
    @Id @Column(length = 50) private String activityId;
    @Column(nullable = false) private String activityName;
    @Column(length = 50) private String unitId;
    @Column(length = 1000) private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registerDeadline;
    private LocalDate cancelDeadline;
    private String location;
    private boolean isLimited;
    private Integer maxParticipants;
    private String createdBy;
    @Enumerated(EnumType.STRING) private Status status;
    private LocalDate createdDate;
}
