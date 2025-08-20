package com.potential_radar.PR.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {
    private String content;
    @JsonProperty("isPrivate")
    private Boolean isPrivate;
    private Long parentId;
}
