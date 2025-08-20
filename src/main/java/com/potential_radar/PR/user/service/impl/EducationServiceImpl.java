package com.potential_radar.PR.user.service.impl;

import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.domain.UserEducation;
import com.potential_radar.PR.user.dto.education.UserEducationRequest;
import com.potential_radar.PR.user.dto.education.UserEducationResponse;
import com.potential_radar.PR.user.repository.UserEducationRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.service.EducationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EducationServiceImpl implements EducationService {
    
    private final UserEducationRepository educationRepository;
    private final UserRepository userRepository;
    
    @Override
    public UserEducationResponse addEducation(String email, UserEducationRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        UserEducation education = UserEducation.builder()
                .user(user)
                .institution(request.institution())
                .program(request.program())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .isCurrent(request.isCurrent())
                .build();
        
        UserEducation saved = educationRepository.save(education);
        return UserEducationResponse.from(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserEducationResponse> getEducations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        return educationRepository.findByUserIdOrderByStartDateDesc(user.getUserId())
                .stream()
                .map(UserEducationResponse::from)
                .toList();
    }
    
    @Override
    public UserEducationResponse updateEducation(String email, Long educationId, UserEducationRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        UserEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("교육 정보를 찾을 수 없습니다: " + educationId));
        
        if (!education.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("해당 교육 정보에 대한 권한이 없습니다");
        }
        
        education.setInstitution(request.institution());
        education.setProgram(request.program());
        education.setStartDate(request.startDate());
        education.setEndDate(request.endDate());
        education.setIsCurrent(request.isCurrent());
        
        UserEducation updated = educationRepository.save(education);
        return UserEducationResponse.from(updated);
    }
    
    @Override
    public void deleteEducation(String email, Long educationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        
        UserEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("교육 정보를 찾을 수 없습니다: " + educationId));
        
        if (!education.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("해당 교육 정보에 대한 권한이 없습니다");
        }
        
        educationRepository.delete(education);
    }
}