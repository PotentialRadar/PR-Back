package com.potential_radar.PR.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = "search_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistoryDocument {
    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String searchType; // "user", "project", "unified"

    @Field(type = FieldType.Text, analyzer = "standard")
    private String searchQuery;

    @Field(type = FieldType.Integer)
    private Integer resultCount;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private String searchTime;

    @Field(type = FieldType.Keyword, index = false)
    private String userAgent;

    @Field(type = FieldType.Keyword, index = false)
    private String ipAddress;
}