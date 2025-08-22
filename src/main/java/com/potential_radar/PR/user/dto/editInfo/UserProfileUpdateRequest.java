package com.potential_radar.PR.user.dto.editInfo;

import com.potential_radar.PR.user.domain.ExperienceRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다")
        String nickname,
        
        Long techPartId,
        
        String profileImage,

        @Size(max = 50, message = "직책은 50자 이하여야 합니다")
        String jobTitle,

        @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자만 10~11자리여야 합니다")
        String phone,
        
        String githubUrl,
        
        String linkedinUrl,
        
        String websiteUrl,
        
        Boolean isPortfolioOpen,
        
        Boolean isContactOpen,
        
        Boolean isSearchOpen,
        
        ExperienceRange experienceRange
) {
}