package com.potential_radar.PR.user.service.impl;

import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserExperience;
import com.potential_radar.PR.user.dto.experience.UserExperienceRequest;
import com.potential_radar.PR.user.dto.experience.UserExperienceResponse;
import com.potential_radar.PR.user.repository.UserExperienceRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExperienceServiceImpl implements ExperienceService {
    
    private final UserExperienceRepository experienceRepository;
    private final UserRepository userRepository;
    
    @Override
    public UserExperienceResponse addExperience(String email, UserExperienceRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        UserExperience experience = UserExperience.builder()
                .user(user)
                .companyName(request.companyName())
                .department(request.department())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isCurrent(request.isCurrent())
                .summary(request.summary())
                .build();
        
        UserExperience saved = experienceRepository.save(experience);
        return UserExperienceResponse.from(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserExperienceResponse> getExperiences(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        return experienceRepository.findByUserIdOrderByStartDateDesc(user.getUserId())
                .stream()
                .map(UserExperienceResponse::from)
                .toList();
    }
    
    @Override
    public UserExperienceResponse updateExperience(String email, Long experienceId, UserExperienceRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        UserExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new IllegalArgumentException("경력 정보를 찾을 수 없습니다: " + experienceId));
        
        if (!experience.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("해당 경력 정보에 대한 권한이 없습니다");
        }
        
        experience.setCompanyName(request.companyName());
        experience.setDepartment(request.department());
        experience.setStartDate(request.startDate());
        experience.setEndDate(request.endDate());
        experience.setIsCurrent(request.isCurrent());
        experience.setSummary(request.summary());
        
        UserExperience updated = experienceRepository.save(experience);
        return UserExperienceResponse.from(updated);
    }
    
    @Override
    public void deleteExperience(String email, Long experienceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        UserExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new IllegalArgumentException("경력 정보를 찾을 수 없습니다: " + experienceId));
        
        if (!experience.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("해당 경력 정보에 대한 권한이 없습니다");
        }
        
        experienceRepository.delete(experience);
    }
}