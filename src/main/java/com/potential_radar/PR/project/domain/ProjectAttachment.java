package com.potential_radar.PR.project.domain;

import com.potential_radar.PR.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "project_attachment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)  //  FK 이름 일치
    private ProjectRecruitment project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private Long size;

    @Builder
    public ProjectAttachment(ProjectRecruitment project, String name, String url, Long size) {
        this.project = project;
        this.name = name;
        this.url = url;
        this.size = size;
    }
}
