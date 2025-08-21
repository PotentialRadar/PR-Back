package com.potential_radar.PR.user.dto.techStack;

import com.potential_radar.PR.user.domain.UserTechStack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTechStackResponse {
    
    private Long userTechStackId;
    private Long stackId;
    private String stackName;
    private Integer skillLevel;
    
    public static UserTechStackResponse from(UserTechStack userTechStack) {
        return UserTechStackResponse.builder()
                .userTechStackId(userTechStack.getUserTechStackId())
                .stackId(userTechStack.getStack().getStackId())
                .stackName(userTechStack.getStack().getName())
                .skillLevel(userTechStack.getSkillLevel())
                .build();
    }
}