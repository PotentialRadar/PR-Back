package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.editPortfolio.UpdatedUserPortfolioResponse;
import com.potential_radar.PR.user.dto.editPortfolio.UserPortfolioUpdateRequest;
import com.potential_radar.PR.user.dto.project.UserAvailableProjectsResponse;
import com.potential_radar.PR.user.dto.project.UserProjectResponse;

import com.potential_radar.PR.user.dto.portfolios.PortfolioSummaryResponse;

import java.util.List;

public interface PortfolioService {
    PortfolioSummaryResponse getPortfolioSummary(Long userId);
    
    UpdatedUserPortfolioResponse getPortfolio(String email);
    
    UpdatedUserPortfolioResponse updatePortfolio(String email, UserPortfolioUpdateRequest request);
    
    void updateBio(String email, String bio);
    
    List<UserAvailableProjectsResponse> getAvailableProjects(String email);
    
    void updateProjectSelection(String email, List<Long> selectedProjectIds);
    
    List<UserProjectResponse> getSelectedProjects(String email);
    
    UserProjectResponse addProjectToPortfolio(String email, Long projectId);
    
    void removeProjectFromPortfolio(String email, Long projectId);
}