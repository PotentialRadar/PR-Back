package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.education.UserEducationRequest;
import com.potential_radar.PR.user.dto.education.UserEducationResponse;

import java.util.List;

public interface EducationService {
    
    UserEducationResponse addEducation(String email, UserEducationRequest request);
    
    List<UserEducationResponse> getEducations(String email);
    
    UserEducationResponse updateEducation(String email, Long educationId, UserEducationRequest request);
    
    void deleteEducation(String email, Long educationId);
}