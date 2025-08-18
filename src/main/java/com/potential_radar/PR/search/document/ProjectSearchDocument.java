package com.potential_radar.PR.search.document;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Document(indexName = "project_search")
@Getter
@NoArgsConstructor
@ToString
public class ProjectSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long projectId;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori") 
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> techParts; // 구하고 있는 기술 파트들

    @Field(type = FieldType.Keyword)
    private List<String> techStacks; // 사용하는 기술 스택들

    @Field(type = FieldType.Keyword)
    private String status; // RECRUITING, COMPLETED 등

    @Field(type = FieldType.Long)
    private Long teamLeaderId;

    @Field(type = FieldType.Text)
    private String teamLeaderNickname;

    @Field(type = FieldType.Integer)
    private Integer recruitCount;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private String recruitDeadline;

    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private String startDate;

    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private String endDate;

    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private String createdAt;

    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private String updatedAt;

    @Builder
    public ProjectSearchDocument(String id, Long projectId, String title, String description,
                               List<String> techParts, List<String> techStacks, String status,
                               Long teamLeaderId, String teamLeaderNickname, Integer recruitCount,
                               Integer viewCount, String recruitDeadline, String startDate,
                               String endDate, String createdAt, String updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.techParts = techParts;
        this.techStacks = techStacks;
        this.status = status;
        this.teamLeaderId = teamLeaderId;
        this.teamLeaderNickname = teamLeaderNickname;
        this.recruitCount = recruitCount;
        this.viewCount = viewCount;
        this.recruitDeadline = recruitDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}