package com.potential_radar.PR.project.service;

import com.potential_radar.PR.common.excetpion.AccessDeniedException;
import com.potential_radar.PR.common.excetpion.DuplicateApplicationException;
import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.dto.ProjectApplicationResponseDTO;
import com.potential_radar.PR.project.dto.ProjectApplyRequest;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectApplicationService {

    private final ProjectApplicationRepository ProjectApplicationRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final UserRepository userRepository;

    // 프로젝트 지원
    @Transactional public void applyProject(Long projectId, ProjectApplyRequest request) {
        if (ProjectApplicationRepository.existsByProject_ProjectIdAndUser_UserId(projectId, request.getUserId())) {
            throw new DuplicateApplicationException("이미 지원하였습니다.");
        }
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        ProjectApplication member = ProjectApplication.builder()
                .project(project)
                .user(user)
                .status(ProjectApplication.ApplicationStatus.PENDING)
                .techPart(request.getTechPart())
                .applicationMessage(request.getApplicationMessage())
                .build();
        ProjectApplicationRepository.save(member);

    }

    // 프로젝트 지원자 목록
    public List<ProjectApplicationResponseDTO> getProjectMembers(Long projectId) {
        List<ProjectApplication> members = ProjectApplicationRepository.findByProject_ProjectId(projectId);
        return members.stream()
                .map(member -> ProjectApplicationResponseDTO.builder()
                        .id(member.getId())
                        .userId(member.getUser().getUserId())
                        .userName(member.getUser().getName()) // 필요하다면
                        .techPart(member.getTechPart())      // 엔티티에 필드 있으면
                        .applicationMessage(member.getApplicationMessage())
                        .status(member.getStatus().name())
                        .build())
                .toList();
    }

    //지원자 상태 업데이트
    @Transactional
    public void updateMemberStatus(Long projectId, Long memberId, Long teamLeaderId, String status) {
        // 1. 프로젝트 조회
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

        // 2. 팀장 검증
        if (!project.getTeamLeader().getUserId().equals(teamLeaderId)) {
            throw new AccessDeniedException("팀장만 승인/거절이 가능합니다.");
        }

        // 3. 지원자 찾기
        ProjectApplication member = ProjectApplicationRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("지원자를 찾을 수 없습니다."));

        // 4. 지원자가 해당 프로젝트 소속인지 검증 (보안)
        if (!member.getProject().getProjectId().equals(projectId)) {
            throw new AccessDeniedException("잘못된 접근입니다.");
        }

        // 5. 상태 변경
        if ("ACCEPTED".equalsIgnoreCase(status)) {
            member.setStatus(ProjectApplication.ApplicationStatus.ACCEPTED);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            member.setStatus(ProjectApplication.ApplicationStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다.");
        }
    }
}
