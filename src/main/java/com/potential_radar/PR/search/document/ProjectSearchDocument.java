package com.potential_radar.PR.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSearchDocument {
    @Id
    private String id;

    private Long projectId;  // 원본 프로젝트 ID

    // 프로젝트명 검색 (한글 분석기 사용)
    @Field(type = FieldType.Text, analyzer = "nori")
    private String projectName;

    // 프로젝트에서 사용하는 기술 스택 (부분 일치 검색)
    @Field(type = FieldType.Text, analyzer = "standard")
    private List<String> techStacks;

    // 구하는 기술 파트 (정확 일치 필터링)
    @Field(type = FieldType.Keyword)
    private List<String> requiredTechParts;

    // 추가 정보 (검색 결과에 표시용)
    private String description;
    private String status;           // RECRUITING, IN_PROGRESS, COMPLETED
    private Long ownerId;
    private String ownerNickname;
    private LocalDateTime createdAt;
}
