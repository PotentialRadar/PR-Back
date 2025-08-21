package com.potential_radar.PR.techStack.domain;

import jakarta.persistence.*;
import lombok.*;

//TODO : dev의 테크스택으로 변경하기 (유저에서 임시 사용하는 TechStack임)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "tech_stack",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tech_stack_name", columnNames = "name")
        }
)
public class TechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stack_id")
    private Long stackId;

    /** 예: Java, Vue, Docker 등 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;
}