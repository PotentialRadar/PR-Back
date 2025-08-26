package com.potential_radar.PR.project.controller;

import com.potential_radar.PR.common.exception.AccessDeniedException;
import com.potential_radar.PR.common.exception.NotFoundException;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.dto.ProjectApplicationResponseDTO;
import com.potential_radar.PR.project.dto.ProjectApplicationStatusUpdateRequest;
import com.potential_radar.PR.project.dto.ProjectApplyRequest;
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.project.service.ProjectApplicationService;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectApplicationController {

    private final ProjectApplicationService projectApplicationService;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final UserRepository userRepository; // UserRepository 주입

    // Authentication 객체에서 사용자 이메일을 추출하는 헬퍼 메서드
    private String getUserEmailFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            // 'anonymousUser'와 같은 경우를 제외
            if ("anonymousUser".equals(principal)) {
                return null;
            }
            return (String) principal;
        }
        return null;
    }

    // [POST] 프로젝트 지원 (body로 받음)
    @PostMapping("/{projectId}/apply")
    public ResponseEntity<String> applyProject(
            @PathVariable Long projectId,
            @RequestBody ProjectApplyRequest request,
            Authentication authentication) {

        String userEmail = getUserEmailFromAuthentication(authentication);
        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        projectApplicationService.applyProject(projectId, request, user.getUserId());
        return ResponseEntity.ok("프로젝트 지원 완료");
    }

    // 프로젝트 지원자 목록 조회
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectApplicationResponseDTO>> getProjectMembers(
            @PathVariable Long projectId,
            @RequestParam Long userId) {
        // 1. 프로젝트 조회
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("해당 프로젝트가 존재하지 않습니다."));

        // 2. 팀리더 검사 (userId가 teamLeader의 userId와 같은지)
        if (!project.getTeamLeader().getUserId().equals(userId)) {
            // 403 Forbidden
            throw new AccessDeniedException("팀장만 지원자 목록을 볼 수 있습니다."); // 또는 커스텀 예외 던져도 됨
        }

        // 3. 지원자 목록 조회
        List<ProjectApplicationResponseDTO> response = projectApplicationService.getProjectMembers(projectId);
        return ResponseEntity.ok(response);
    }

    // 사용자가 지원한 프로젝트 목록 조회
    @GetMapping("/users/{userId}/applied")
    public ResponseEntity<List<ProjectRecruitmentResponse>> getAppliedProjectsByUser(@PathVariable Long userId) {
        List<ProjectRecruitmentResponse> response = projectApplicationService.getAppliedProjectsByUser(userId);
        return ResponseEntity.ok(response);
    }

    //지원자 승인/거절 업데이트
    @PatchMapping("/{projectId}/members/{memberId}/status")
    public ResponseEntity<String> updateMemberStatus(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestParam Long userId,      // 팀장 ID
            @RequestBody ProjectApplicationStatusUpdateRequest request
    ) {
        projectApplicationService.updateMemberStatus(projectId, memberId, userId, request.getStatus());
        return ResponseEntity.ok("지원자 상태가 변경되었습니다.");
    }

}
