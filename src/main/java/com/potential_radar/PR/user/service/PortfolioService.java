package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.dto.editPortfolio.UpdatedUserPortfolioResponse;
import com.potential_radar.PR.user.dto.editPortfolio.UserPortfolioUpdateRequest;

public interface PortfolioService {
    
    UpdatedUserPortfolioResponse getPortfolio(String email);
    
    UpdatedUserPortfolioResponse updatePortfolio(String email, UserPortfolioUpdateRequest request);
}