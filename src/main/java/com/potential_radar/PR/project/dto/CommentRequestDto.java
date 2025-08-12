package com.potential_radar.PR.project.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {
    private String content;
    private boolean isPrivate;
    private Long parentId;
}
