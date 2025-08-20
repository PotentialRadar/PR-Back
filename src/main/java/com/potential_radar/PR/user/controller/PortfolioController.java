package com.potential_radar.PR.user.controller;

import com.potential_radar.PR.user.dto.editPortfolio.UpdatedUserPortfolioResponse;
import com.potential_radar.PR.user.dto.editPortfolio.UserPortfolioUpdateRequest;
import com.potential_radar.PR.user.dto.education.UserEducationRequest;
import com.potential_radar.PR.user.dto.education.UserEducationResponse;
import com.potential_radar.PR.user.dto.experience.UserExperienceRequest;
import com.potential_radar.PR.user.dto.experience.UserExperienceResponse;
import com.potential_radar.PR.user.service.EducationService;
import com.potential_radar.PR.user.service.ExperienceService;
import com.potential_radar.PR.user.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Slf4j
public class PortfolioController {

    private final EducationService educationService;
    private final ExperienceService experienceService;
    private final PortfolioService portfolioService;

    // 전체 포트폴리오 조회
    @GetMapping("/portfolio")
    public ResponseEntity<UpdatedUserPortfolioResponse> getPortfolio(Principal principal) {
        String email = principal.getName();
        UpdatedUserPortfolioResponse portfolio = portfolioService.getPortfolio(email);
        return ResponseEntity.ok(portfolio);
    }
    
    // 포트폴리오 통합 업데이트 (자기소개, 교육, 경력)
    @PutMapping("/portfolio")
    public ResponseEntity<UpdatedUserPortfolioResponse> updatePortfolio(
            @Valid @RequestBody UserPortfolioUpdateRequest request,
            Principal principal) {
        String email = principal.getName();
        UpdatedUserPortfolioResponse response = portfolioService.updatePortfolio(email, request);
        return ResponseEntity.ok(response);
    }

    // === 교육 정보 CRUD ===
    
    @PostMapping("/educations")
    public ResponseEntity<UserEducationResponse> addEducation(
            @Valid @RequestBody UserEducationRequest request,
            Principal principal) {
        String email = principal.getName();
        UserEducationResponse response = educationService.addEducation(email, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/educations")
    public ResponseEntity<List<UserEducationResponse>> getEducations(Principal principal) {
        String email = principal.getName();
        List<UserEducationResponse> educations = educationService.getEducations(email);
        return ResponseEntity.ok(educations);
    }
    
    @PutMapping("/educations/{educationId}")
    public ResponseEntity<UserEducationResponse> updateEducation(
            @PathVariable Long educationId,
            @Valid @RequestBody UserEducationRequest request,
            Principal principal) {
        String email = principal.getName();
        UserEducationResponse response = educationService.updateEducation(email, educationId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/educations/{educationId}")
    public ResponseEntity<Object> deleteEducation(
            @PathVariable Long educationId,
            Principal principal) {
        String email = principal.getName();
        educationService.deleteEducation(email, educationId);
        return ResponseEntity.ok(Map.of("message", "교육 정보가 삭제되었습니다"));
    }

    // === 경력 정보 CRUD ===
    
    @PostMapping("/experiences")
    public ResponseEntity<UserExperienceResponse> addExperience(
            @Valid @RequestBody UserExperienceRequest request,
            Principal principal) {
        String email = principal.getName();
        UserExperienceResponse response = experienceService.addExperience(email, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/experiences")
    public ResponseEntity<List<UserExperienceResponse>> getExperiences(Principal principal) {
        String email = principal.getName();
        List<UserExperienceResponse> experiences = experienceService.getExperiences(email);
        return ResponseEntity.ok(experiences);
    }
    
    @PutMapping("/experiences/{experienceId}")
    public ResponseEntity<UserExperienceResponse> updateExperience(
            @PathVariable Long experienceId,
            @Valid @RequestBody UserExperienceRequest request,
            Principal principal) {
        String email = principal.getName();
        UserExperienceResponse response = experienceService.updateExperience(email, experienceId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/experiences/{experienceId}")
    public ResponseEntity<Object> deleteExperience(
            @PathVariable Long experienceId,
            Principal principal) {
        String email = principal.getName();
        experienceService.deleteExperience(email, experienceId);
        return ResponseEntity.ok(Map.of("message", "경력 정보가 삭제되었습니다"));
    }
}