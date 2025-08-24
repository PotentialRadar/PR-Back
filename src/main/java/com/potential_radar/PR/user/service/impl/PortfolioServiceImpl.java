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
        
        // 1. 프로필 자기소개 업데이트
        UserProfile profile = user.getUserProfile();
        if (profile != null && request.bio() != null) {
            profile.setBio(request.bio());
            userProfileRepository.save(profile);
        }
        
        // 2. 기존 교육 정보 삭제 후 새로 추가
        educationRepository.deleteAll(user.getEducations());
        user.getEducations().clear();
        
        if (request.educations() != null) {
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
        
        // 3. 기존 경력 정보 삭제 후 새로 추가
        experienceRepository.deleteAll(user.getExperiences());
        user.getExperiences().clear();
        
        if (request.experiences() != null) {
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
        
        // 4. 기존 기술 스택 정보 삭제 후 새로 추가
        techStackRepository.deleteAll(user.getUserTechStacks());
        user.getUserTechStacks().clear();
        
        if (request.techStacks() != null) {
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