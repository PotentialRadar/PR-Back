package com.potential_radar.PR.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_experience")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "experience_id")
    private Long experienceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, columnDefinition = "VARCHAR COMMENT '회사이름'")
    private String companyName;

    @Column(name = "department", columnDefinition = "VARCHAR COMMENT '부서, 직책'")
    private String department;

    @Column(name = "start_date", nullable = false, columnDefinition = "DATE COMMENT '입사일'")
    private LocalDate startDate;

    @Column(name = "end_date", columnDefinition = "DATE COMMENT '퇴사일'")
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE COMMENT '재직여부'")
    @Builder.Default
    private Boolean isCurrent = false;

    @Column(name = "summary", columnDefinition = "VARCHAR COMMENT '소개'")
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP COMMENT '생성일'")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP COMMENT '수정일'")
    private LocalDateTime updatedAt;
}