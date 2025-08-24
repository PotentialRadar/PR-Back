package com.potential_radar.PR.user.dto.techStack;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserTechStackRequest {
    
    @NotNull(message = "기술 스택 ID는 필수입니다")
    private Long stackId;
    
    @Min(value = 1, message = "숙련도는 1 이상이어야 합니다")
    @Max(value = 5, message = "숙련도는 5 이하여야 합니다")
    private Integer skillLevel;
}