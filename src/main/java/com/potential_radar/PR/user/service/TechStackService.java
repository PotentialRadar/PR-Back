package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.techStack.UserTechStackRequest;
import com.potential_radar.PR.user.dto.techStack.UserTechStackResponse;

import java.util.List;

public interface TechStackService {
    
    UserTechStackResponse addTechStack(String userEmail, UserTechStackRequest request);
    
    List<UserTechStackResponse> getTechStacks(String userEmail);
    
    UserTechStackResponse updateTechStack(String userEmail, Long userTechStackId, UserTechStackRequest request);
    
    void deleteTechStack(String userEmail, Long userTechStackId);
}