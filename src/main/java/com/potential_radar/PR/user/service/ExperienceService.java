package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.experience.UserExperienceRequest;
import com.potential_radar.PR.user.dto.experience.UserExperienceResponse;

import java.util.List;

public interface ExperienceService {
    
    UserExperienceResponse addExperience(String email, UserExperienceRequest request);
    
    List<UserExperienceResponse> getExperiences(String email);
    
    UserExperienceResponse updateExperience(String email, Long experienceId, UserExperienceRequest request);
    
    void deleteExperience(String email, Long experienceId);
}