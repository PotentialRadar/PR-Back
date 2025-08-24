package com.potential_radar.PR.user.service.impl;

import com.potential_radar.PR.project.domain.ProjectMember;
import com.potential_radar.PR.project.repository.ProjectMemberRepository;
import com.potential_radar.PR.project.repository.ProjectTechStackRepository;
import com.potential_radar.PR.user.domain.UserProfile;
import com.potential_radar.PR.user.dto.education.UserEducationResponse;
import com.potential_radar.PR.user.dto.experience.UserExperienceResponse;
import com.potential_radar.PR.user.dto.myPortfolio.OfficialPotfolioResponse;
import com.potential_radar.PR.user.dto.project.UserProjectResponse;
import com.potential_radar.PR.user.dto.techStack.UserTechStackResponse;
import com.potential_radar.PR.user.repository.UserEducationRepository;
import com.potential_radar.PR.user.repository.UserExperienceRepository;
import com.potential_radar.PR.user.repository.UserProfileRepository;
import com.potential_radar.PR.user.repository.UserTechStackRepository;
import com.potential_radar.PR.user.service.PublicPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PublicPortfolioServiceImpl implements PublicPortfolioService {
    
    private final UserProfileRepository userProfileRepository;
    private final UserEducationRepository userEducationRepository;
    private final UserExperienceRepository userExperienceRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTechStackRepository projectTechStackRepository;
    
    @Override
    public OfficialPotfolioResponse getPublicPortfolio(Long portfolioId) {
        log.info("공개 포트폴리오 조회 요청: portfolioId = {}", portfolioId);
        
        UserProfile userProfile = userProfileRepository.findPublicPortfolioByUserId(portfolioId)
                .orElseThrow(() -> {
                    log.warn("공개되지 않았거나 존재하지 않는 포트폴리오: portfolioId = {}", portfolioId);
                    return new IllegalArgumentException("해당 포트폴리오는 공개되지 않았거나 존재하지 않습니다.");
                });
        
        // 교육 이력 조회
        List<UserEducationResponse> educations = userEducationRepository
                .findByUserIdOrderByStartDateDesc(portfolioId)
                .stream()
                .map(UserEducationResponse::from)
                .toList();
        
        // 경력 이력 조회
        List<UserExperienceResponse> experiences = userExperienceRepository
                .findByUserIdOrderByStartDateDesc(portfolioId)
                .stream()
                .map(UserExperienceResponse::from)
                .toList();
        
        // 기술 스택 조회
        List<UserTechStackResponse> techStacks = userTechStackRepository
                .findByUserWithTechStack(userProfile.getUser())
                .stream()
                .map(UserTechStackResponse::from)
                .toList();
        
        // 프로젝트 이력 조회
        List<UserProjectResponse> projects = getUserProjects(portfolioId);
        
        log.info("공개 포트폴리오 조회 성공: userId = {}, nickname = {}, 교육 {개}, 경력 {}개, 기술스택 {}개, 프로젝트 {}개", 
                userProfile.getUser().getUserId(), 
                userProfile.getUser().getNickname(),
                educations.size(),
                experiences.size(),
                techStacks.size(),
                projects.size());
        
        return new OfficialPotfolioResponse(userProfile, userProfile.getUser(), 
                educations, experiences, techStacks, projects);
    }
    
    private List<UserProjectResponse> getUserProjects(Long userId) {
        List<ProjectMember> projectMembers = projectMemberRepository.findAllByUser_UserId(userId);
        
        return projectMembers.stream()
                .map(member -> {
                    List<String> techStacks = projectTechStackRepository
                            .findByProject_ProjectId(member.getProject().getProjectId())
                            .stream()
                            .map(pts -> pts.getTechStack().getName())
                            .toList();
                    
                    return UserProjectResponse.builder()
                            .projectId(member.getProject().getProjectId())
                            .title(member.getProject().getTitle())
                            .description(member.getProject().getDescription())
                            .status(member.getProject().getStatus().name())
                            .role(member.getRole().name())
                            .techPart(member.getTechPart())
                            .startDate(member.getProject().getStartDate())
                            .endDate(member.getProject().getEndDate())
                            .techStacks(techStacks)
                            .build();
                })
                .toList();
    }
}