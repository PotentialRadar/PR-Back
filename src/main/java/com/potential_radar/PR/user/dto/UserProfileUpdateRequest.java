package com.potential_radar.PR.user.dto;

import com.potential_radar.PR.user.model.ExperienceRange;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다")
        String nickname,
        
        Long techPartId,
        
        String profileImage,
        
        @Size(max = 500, message = "자기소개는 500자 이하여야 합니다")
        String bio,
        
        @Size(max = 100, message = "한줄소개는 100자 이하여야 합니다")
        String bioShort,
        
        String phone,
        
        String githubUrl,
        
        String linkedinUrl,
        
        String websiteUrl,
        
        String region,
        
        Boolean isPortfolioOpen,
        
        Boolean isContactOpen,
        
        Boolean isSearchOpen,
        
        ExperienceRange experienceRange
) {
}