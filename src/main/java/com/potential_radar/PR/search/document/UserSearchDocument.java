package com.potential_radar.PR.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "users")
@Setting(settingPath = "/elasticsearch/user-index-settings.json")
@Mapping(mappingPath = "/elasticsearch/user-index-mapping.json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchDocument {
    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long userId;

    // ✅ 커스텀 한글 분석기 사용
    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String nickname;

    @Field(type = FieldType.Keyword)
    private String techPart;

    // ✅ 커스텀 기술스택 분석기 사용
    @Field(type = FieldType.Text, analyzer = "tech_stack_analyzer")
    private List<String> techStacks;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String introduction;

    @Field(type = FieldType.Keyword, index = false)
    private String profileImage;

    @Field(type = FieldType.Keyword, index = false)
    private String githubUrl;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String region;

    @Field(type = FieldType.Boolean)
    private Boolean isSearchable = true;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
}
