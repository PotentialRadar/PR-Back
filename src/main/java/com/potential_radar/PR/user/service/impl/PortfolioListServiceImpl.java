package com.potential_radar.PR.user.service.impl;

import com.potential_radar.PR.project.repository.ProjectMemberRepository;
import com.potential_radar.PR.user.domain.UserProfile;
import com.potential_radar.PR.user.dto.portfolios.PortfolioListResponse;
import com.potential_radar.PR.user.dto.portfolios.PortfolioSearchRequest;
import com.potential_radar.PR.user.dto.portfolios.PortfolioSummaryResponse;
import com.potential_radar.PR.user.repository.UserProfileRepository;
import com.potential_radar.PR.user.repository.UserTechStackRepository;
import com.potential_radar.PR.user.service.PortfolioListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PortfolioListServiceImpl implements PortfolioListService {
    
    private final UserProfileRepository userProfileRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserTechStackRepository userTechStackRepository;
    
    @Override
    public PortfolioListResponse getPublicPortfolios(PortfolioSearchRequest searchRequest) {
        log.info("공개 포트폴리오 목록 조회 요청: {}", searchRequest);
        
        // 기본값 적용
        PortfolioSearchRequest request = searchRequest.withDefaults();
        
        // 페이지 정보 생성
        Pageable pageable = createPageable(request);
        
        // 필터 조건에 따라 다른 쿼리 사용
        Page<UserProfile> userProfiles = findUserProfilesWithFilters(request, pageable);
        
        // UserProfile을 PortfolioSummaryResponse로 변환
        Page<PortfolioSummaryResponse> portfolioPage = userProfiles.map(this::convertToSummaryResponse);
        
        log.info("공개 포트폴리오 목록 조회 완료: 총 {}개, 현재 페이지 {}/{}", 
                portfolioPage.getTotalElements(), 
                portfolioPage.getNumber() + 1, 
                portfolioPage.getTotalPages());
        
        return PortfolioListResponse.from(portfolioPage);
    }
    
    private Page<UserProfile> findUserProfilesWithFilters(PortfolioSearchRequest request, Pageable pageable) {
        boolean hasTechPart = request.techPart() != null && !request.techPart().trim().isEmpty();
        boolean hasExperienceRange = request.experienceRange() != null;
        boolean hasKeyword = request.keyword() != null && !request.keyword().trim().isEmpty();
        
        // 필터가 있는 경우 복합 검색 쿼리 사용
        if (hasTechPart || hasExperienceRange || hasKeyword) {
            return userProfileRepository.findPublicPortfoliosWithFilters(
                    hasTechPart ? request.techPart().trim() : null,
                    hasExperienceRange ? request.experienceRange() : null,
                    hasKeyword ? request.keyword().trim() : null,
                    pageable
            );
        }
        
        // 필터가 없으면 전체 조회
        return userProfileRepository.findAllPublicPortfolios(pageable);
    }
    
    private Pageable createPageable(PortfolioSearchRequest request) {
        Sort sort = createSort(request.sortBy());
        return PageRequest.of(request.page(), request.size(), sort);
    }
    
    private Sort createSort(String sortBy) {
        return switch (sortBy) {
            case "reviewCount" -> Sort.by(Sort.Direction.DESC, "reviewCount")
                    .and(Sort.by(Sort.Direction.DESC, "reputationScore"));
            case "recent" -> Sort.by(Sort.Direction.DESC, "user.createdAt");
            default -> Sort.by(Sort.Direction.DESC, "reputationScore")
                    .and(Sort.by(Sort.Direction.DESC, "reviewCount"));
        };
    }
    
    private PortfolioSummaryResponse convertToSummaryResponse(UserProfile userProfile) {
        // 프로젝트 개수 조회 (사용자가 참여한 프로젝트 수)
        int projectCount = projectMemberRepository.findAllByUser_UserId(userProfile.getUserId()).size();
        
        // 기술 스택 개수 조회
        int techStackCount = userTechStackRepository.findByUserWithTechStack(userProfile.getUser()).size();
        
        return PortfolioSummaryResponse.from(userProfile, projectCount, techStackCount);
    }
}