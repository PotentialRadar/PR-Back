package com.potential_radar.PR.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponseDto {
    private Long commentId;
    private Long userId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonProperty("isPrivate")
    private boolean isPrivate;
    private List<CommentResponseDto> children;
}
