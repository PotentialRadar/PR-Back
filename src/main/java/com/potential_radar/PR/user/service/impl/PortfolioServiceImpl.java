package com.potential_radar.PR.user.service.impl;

import com.potential_radar.PR.project.domain.ProjectMember;
import com.potential_radar.PR.project.repository.ProjectMemberRepository;
import com.potential_radar.PR.project.repository.ProjectTechStackRepository;
import com.potential_radar.PR.tech.domain.TechStack;
import com.potential_radar.PR.tech.repository.TechStackRepository;
import com.potential_radar.PR.user.domain.*;
import com.potential_radar.PR.user.dto.editPortfolio.UpdatedUserPortfolioResponse;
import com.potential_radar.PR.user.dto.editPortfolio.UserPortfolioUpdateRequest;
import com.potential_radar.PR.user.dto.education.UserEducationRequest;
import com.potential_radar.PR.user.dto.education.UserEducationResponse;
import com.potential_radar.PR.user.dto.experience.UserExperienceRequest;
import com.potential_radar.PR.user.dto.experience.UserExperienceResponse;
import com.potential_radar.PR.user.dto.project.UserAvailableProjectsResponse;
import com.potential_radar.PR.user.dto.project.UserProjectResponse;
import com.potential_radar.PR.user.dto.techStack.UserTechStackRequest;
import com.potential_radar.PR.user.dto.techStack.UserTechStackResponse;
import com.potential_radar.PR.user.repository.*;
import com.potential_radar.PR.user.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioServiceImpl implements PortfolioService {
    
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserEducationRepository educationRepository;
    private final UserExperienceRepository experienceRepository;
    private final UserTechStackRepository techStackRepository;
    private final TechStackRepository stackRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTechStackRepository projectTechStackRepository;
    private final PortfolioProjectRepository portfolioProjectRepository;
    
    @Override
    public UpdatedUserPortfolioResponse getPortfolio(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        UserProfile profile = user.getUserProfile();
        
        List<UserEducationResponse> educations = educationRepository
                .findByUserIdOrderByStartDateDesc(user.getUserId())
                .stream()
                .map(UserEducationResponse::from)
                .toList();
        
        List<UserExperienceResponse> experiences = experienceRepository
                .findByUserIdOrderByStartDateDesc(user.getUserId())
                .stream()
                .map(UserExperienceResponse::from)
                .toList();
        
        List<UserTechStackResponse> techStacks = techStackRepository
                .findByUserWithTechStack(user)
                .stream()
                .map(UserTechStackResponse::from)
                .toList();
        
        List<UserProjectResponse> projects = getUserProjects(user.getUserId());
        
        return new UpdatedUserPortfolioResponse(
                user.getUserId(),
                user.getProfileImage(),
                user.getNickname(),
                profile != null && profile.getTechPart() != null ? profile.getTechPart().getName() : null,
                profile != null ? profile.getJobTitle() : null,
                profile != null ? profile.getBio() : null,
                educations,
                experiences,
                techStacks,
                projects
        );
    }
    
    @Override
    @Transactional
    public UpdatedUserPortfolioResponse updatePortfolio(String email, UserPortfolioUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        // 1. 프로필 자기소개 업데이트 (null이 아닌 경우에만)
        UserProfile profile = user.getUserProfile();
        if (profile != null && request.bio() != null) {
            profile.setBio(request.bio());
            userProfileRepository.save(profile);
        }
        
        // 2. 교육 정보 업데이트 (null이 아니고 비어있지 않은 경우에만 교체)
        if (request.educations() != null) {
            // 기존 교육 정보 삭제 후 새로 추가
            educationRepository.deleteAll(user.getEducations());
            user.getEducations().clear();
            
            for (UserEducationRequest eduReq : request.educations()) {
                UserEducation education = UserEducation.builder()
                        .user(user)
                        .institution(eduReq.institution())
                        .program(eduReq.program())
                        .startDate(eduReq.startDate())
                        .endDate(eduReq.endDate())
                        .isCurrent(eduReq.isCurrent())
                        .build();
                user.addEducation(education);
            }
        }
        
        // 3. 경력 정보 업데이트 (null이 아니고 비어있지 않은 경우에만 교체)
        if (request.experiences() != null) {
            // 기존 경력 정보 삭제 후 새로 추가
            experienceRepository.deleteAll(user.getExperiences());
            user.getExperiences().clear();
            
            for (UserExperienceRequest expReq : request.experiences()) {
                UserExperience experience = UserExperience.builder()
                        .user(user)
                        .companyName(expReq.companyName())
                        .department(expReq.department())
                        .startDate(expReq.startDate())
                        .endDate(expReq.endDate())
                        .isCurrent(expReq.isCurrent())
                        .summary(expReq.summary())
                        .build();
                user.addExperience(experience);
            }
        }
        
        // 4. 기술 스택 정보 업데이트 (null이 아니고 비어있지 않은 경우에만 교체)
        if (request.techStacks() != null) {
            // 기존 기술 스택 정보 삭제 후 새로 추가
            techStackRepository.deleteAll(user.getUserTechStacks());
            user.getUserTechStacks().clear();
            
            for (UserTechStackRequest techReq : request.techStacks()) {
                TechStack techStack = stackRepository.findById(techReq.getStackId())
                        .orElseThrow(() -> new IllegalArgumentException("기술 스택을 찾을 수 없습니다: " + techReq.getStackId()));
                
                UserTechStack userTechStack = UserTechStack.builder()
                        .user(user)
                        .stack(techStack)
                        .skillLevel(techReq.getSkillLevel())
                        .build();
                user.getUserTechStacks().add(userTechStack);
            }
        }
        
        // 5. 프로젝트 선택 업데이트 (null이 아닌 경우에만)
        if (request.selectedProjectIds() != null) {
            updateProjectSelection(email, request.selectedProjectIds());
        }
        
        userRepository.save(user);
        
        // 업데이트된 정보 반환
        return getPortfolio(email);
    }
    
    @Override
    @Transactional
    public void updateBio(String email, String bio) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            profile.setBio(bio);
            userProfileRepository.save(profile);
        }
    }
    
    private List<UserProjectResponse> getUserProjects(Long userId) {
        List<Long> selectedProjectIds = portfolioProjectRepository.findProjectIdsByUserId(userId);
        
        List<ProjectMember> projectMembers = projectMemberRepository.findAllByUser_UserId(userId);
        
        return projectMembers.stream()
                .filter(member -> selectedProjectIds.contains(member.getProject().getProjectId()))
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

    @Override
    public List<UserAvailableProjectsResponse> getAvailableProjects(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        List<Long> selectedProjectIds = portfolioProjectRepository.findProjectIdsByUserId(user.getUserId());
        List<ProjectMember> projectMembers = projectMemberRepository.findAllByUser_UserId(user.getUserId());
        
        return projectMembers.stream()
                .map(member -> {
                    List<String> techStacks = projectTechStackRepository
                            .findByProject_ProjectId(member.getProject().getProjectId())
                            .stream()
                            .map(pts -> pts.getTechStack().getName())
                            .toList();
                    
                    return UserAvailableProjectsResponse.builder()
                            .projectId(member.getProject().getProjectId())
                            .title(member.getProject().getTitle())
                            .description(member.getProject().getDescription())
                            .status(member.getProject().getStatus().name())
                            .role(member.getRole().name())
                            .techPart(member.getTechPart())
                            .startDate(member.getProject().getStartDate())
                            .endDate(member.getProject().getEndDate())
                            .techStacks(techStacks)
                            .selectedInPortfolio(selectedProjectIds.contains(member.getProject().getProjectId()))
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public void updateProjectSelection(String email, List<Long> selectedProjectIds) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        portfolioProjectRepository.deleteAllByUserId(user.getUserId());
        
        if (selectedProjectIds != null && !selectedProjectIds.isEmpty()) {
            List<ProjectMember> userProjectMembers = projectMemberRepository.findAllByUser_UserId(user.getUserId());
            List<Long> userProjectIds = userProjectMembers.stream()
                    .map(member -> member.getProject().getProjectId())
                    .toList();
            
            List<PortfolioProject> portfolioProjects = selectedProjectIds.stream()
                    .filter(userProjectIds::contains)
                    .map(projectId -> {
                        ProjectMember projectMember = userProjectMembers.stream()
                                .filter(member -> member.getProject().getProjectId().equals(projectId))
                                .findFirst()
                                .orElseThrow();
                        
                        return PortfolioProject.builder()
                                .user(user)
                                .project(projectMember.getProject())
                                .build();
                    })
                    .toList();
            
            portfolioProjectRepository.saveAll(portfolioProjects);
        }
    }
    
    @Override
    public List<UserProjectResponse> getSelectedProjects(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        return getUserProjects(user.getUserId());
    }
    
    @Override
    @Transactional
    public UserProjectResponse addProjectToPortfolio(String email, Long projectId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        // 사용자가 해당 프로젝트의 멤버인지 확인
        ProjectMember projectMember = projectMemberRepository.findAllByUser_UserId(user.getUserId())
                .stream()
                .filter(member -> member.getProject().getProjectId().equals(projectId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 프로젝트의 멤버가 아닙니다"));
        
        // 이미 포트폴리오에 추가되어 있는지 확인
        if (portfolioProjectRepository.existsByUser_UserIdAndProject_ProjectId(user.getUserId(), projectId)) {
            throw new IllegalArgumentException("이미 포트폴리오에 추가된 프로젝트입니다");
        }
        
        // 포트폴리오에 프로젝트 추가
        PortfolioProject portfolioProject = PortfolioProject.builder()
                .user(user)
                .project(projectMember.getProject())
                .build();
        
        portfolioProjectRepository.save(portfolioProject);
        
        // UserProjectResponse 생성해서 반환
        List<String> techStacks = projectTechStackRepository
                .findByProject_ProjectId(projectId)
                .stream()
                .map(pts -> pts.getTechStack().getName())
                .toList();
        
        return UserProjectResponse.builder()
                .projectId(projectMember.getProject().getProjectId())
                .title(projectMember.getProject().getTitle())
                .description(projectMember.getProject().getDescription())
                .status(projectMember.getProject().getStatus().name())
                .role(projectMember.getRole().name())
                .techPart(projectMember.getTechPart())
                .startDate(projectMember.getProject().getStartDate())
                .endDate(projectMember.getProject().getEndDate())
                .techStacks(techStacks)
                .build();
    }
    
    @Override
    @Transactional
    public void removeProjectFromPortfolio(String email, Long projectId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        // 포트폴리오에서 프로젝트 제거
        List<PortfolioProject> portfolioProjects = portfolioProjectRepository.findAllByUser_UserId(user.getUserId());
        PortfolioProject portfolioProject = portfolioProjects.stream()
                .filter(pp -> pp.getProject().getProjectId().equals(projectId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오에 해당 프로젝트가 없습니다"));
        
        portfolioProjectRepository.delete(portfolioProject);
    }
}