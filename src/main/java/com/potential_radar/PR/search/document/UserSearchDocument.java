package com.potential_radar.PR.search.document;

import com.potential_radar.PR.user.domain.ExperienceRange;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

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


    // 표준 분석기 사용
    @Field(type = FieldType.Text, analyzer = "standard")
    private String nickname;

    @Field(type = FieldType.Keyword)
    private String techPart;

    // 커스텀 기술스택 분석기 사용
    @Field(type = FieldType.Text, analyzer = "tech_stack_analyzer")
    private List<String> techStacks;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String introduction;

    @Field(type = FieldType.Keyword, index = false)
    private String profileImage;

    @Field(type = FieldType.Keyword, index = false)
    private String githubUrl;

    @Field(type = FieldType.Keyword)
    private ExperienceRange experienceRange;

    @Field(type = FieldType.Boolean)
    private Boolean isSearchable = true;

    @Field(type = FieldType.Boolean)
    private Boolean isPortfolioOpen = true;

    @Field(type = FieldType.Boolean)
    private Boolean isSearchOpen = true;

    @Field(type = FieldType.Text, index = false)
    private String createdAt;

    @Field(type = FieldType.Text, index = false)
    private String updatedAt;
}
