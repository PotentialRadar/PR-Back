package com.potential_radar.PR.user.domain;

<<<<<<< HEAD

import com.potential_radar.PR.techStack.domain.TechStack;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "user_tech_stack",
        uniqueConstraints = {
                // 한 유저가 동일 스택을 중복으로 가지지 못하도록
                @UniqueConstraint(name = "uk_user_tech_stack_user_stack", columnNames = {"user_id", "stack_id"})
        },
        indexes = {
                @Index(name = "idx_user_tech_stack_user_id", columnList = "user_id"),
                @Index(name = "idx_user_tech_stack_stack_id", columnList = "stack_id")
        }
)
public class UserTechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_tech_stack_id")
    private Long userTechStackId;

    /** FK: users.user_id */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_tech_stack_user"))
    private User user;

    /** FK: tech_stack.stack_id */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stack_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_tech_stack_stack"))
    private TechStack stack;

    /** 숙련도 1~5 */
    @Column(name = "skill_level")
    @Min(1) @Max(5)
    private Integer skillLevel; // NULL 허용(미평가) → ERD의 "NULL, 1~5" 반영
}
=======
import com.potential_radar.PR.tech.domain.TechStack;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_tech_stack")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTechStack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userTechStackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_stack_id")
    private TechStack techStack;

    @Column(nullable = true)
    private Integer skillLevel;
}

>>>>>>> origin/dev
