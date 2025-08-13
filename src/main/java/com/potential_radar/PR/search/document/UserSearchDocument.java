package com.potential_radar.PR.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Document(indexName = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchDocument {
    @Id
    private String id;

    private Long userId;  // 원본 사용자 ID

    // 사용자 닉네임 검색 (한글 분석기 사용)
    @Field(type = FieldType.Text, analyzer = "nori")
    private String nickname;

    // 사용자의 기술 파트 (정확 일치 필터링)
    @Field(type = FieldType.Keyword)
    private String techPart;

    // 사용자의 기술 스택 (부분 일치 검색)
    @Field(type = FieldType.Text, analyzer = "standard")
    private List<String> techStacks;

    // 추가 정보 (검색 결과에 표시용)
    private String bio;
    private String profileImage;
    private Boolean isSearchOpen = true;  // 검색 허용 여부
}
