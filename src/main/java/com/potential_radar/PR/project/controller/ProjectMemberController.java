package com.potential_radar.PR.project.controller;

import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.project.domain.ProjectMember;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.dto.ProjectApplyRequest;
import com.potential_radar.PR.project.dto.ProjectMemberResponseDTO;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.project.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;

    // [POST] 프로젝트 지원 (body로 받음)
    @PostMapping("/{projectId}/apply")
    public ResponseEntity<String> applyProject(
            @PathVariable Long projectId,
            @RequestBody ProjectApplyRequest request) {  // Body로 받도록 변경!
        projectMemberService.applyProject(projectId, request);
        return ResponseEntity.ok("프로젝트 지원 완료");
    }

    // 프로젝트 지원자 목록 조회
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponseDTO>> getProjectMembers(
            @PathVariable Long projectId,
            @RequestParam Long userId) {
        // 1. 프로젝트 조회
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("해당 프로젝트가 존재하지 않습니다."));

        // 2. 팀리더 검사 (userId가 teamLeader의 userId와 같은지)
        if (!project.getTeamLeader().getUserId().equals(userId)) {
            // 403 Forbidden
            return ResponseEntity.status(403).body(null); // 또는 커스텀 예외 던져도 됨
        }

        // 3. 지원자 목록 조회
        List<ProjectMemberResponseDTO> response = projectMemberService.getProjectMembers(projectId);
        return ResponseEntity.ok(response);
    }
}
