package com.potential_radar.PR.project.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAttachmentDto {
    private Long fileId;
    private String name;
    private String url;
    private Long size;
}
