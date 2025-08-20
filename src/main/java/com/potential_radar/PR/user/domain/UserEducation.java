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
@Table(name = "user_education")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_id")
    private Long educationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "institution", nullable = false, columnDefinition = "VARCHAR COMMENT '기관이름'")
    private String institution;

    @Column(name = "program", columnDefinition = "VARCHAR COMMENT '전공, 과정'")
    private String program;

    @Column(name = "start_date", nullable = false, columnDefinition = "DATE COMMENT '입학일'")
    private LocalDate startDate;

    @Column(name = "end_date", columnDefinition = "DATE COMMENT '졸업일'")
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE COMMENT '재학여부'")
    @Builder.Default
    private Boolean isCurrent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP COMMENT '생성일'")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP COMMENT '수정일'")
    private LocalDateTime updatedAt;
}
