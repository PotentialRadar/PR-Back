package com.potential_radar.PR.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;


@Document(indexName = "projects")
@Setting(settingPath = "/elasticsearch/project-index-settings.json")
@Mapping(mappingPath = "/elasticsearch/project-index-mapping.json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSearchDocument {
    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long projectId;

    // ✅ 커스텀 한글 분석기 사용
    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String projectName;

    // ✅ 커스텀 기술스택 분석기 사용
    @Field(type = FieldType.Text, analyzer = "tech_stack_analyzer")
    private List<String> techStacks;

    @Field(type = FieldType.Keyword)
    private List<String> requiredTechParts;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String description;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private Long ownerId;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String ownerNickname;

    @Field(type = FieldType.Text, index = false)
    private String createdAt;

    @Field(type = FieldType.Text, index = false)
    private String updatedAt;
}
