package com.potential_radar.PR.project.service;

import com.potential_radar.PR.common.excetpion.AccessDeniedException;
import com.potential_radar.PR.common.excetpion.DuplicateApplicationException;
import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.dto.ProjectApplicationResponseDTO;
import com.potential_radar.PR.project.dto.ProjectApplyRequest;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectApplicationService {

    private final ProjectApplicationRepository ProjectApplicationRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectMemberRepository projectMemberRepository; // ✅ 멤버 저장소
    private final UserRepository userRepository;

    // 프로젝트 지원
    @Transactional
    public void applyProject(Long projectId, ProjectApplyRequest request) {
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
    @Transactional(readOnly = true)
    public List<ProjectApplicationResponseDTO> getProjectMembers(Long projectId) {
        List<ProjectApplication> members = ProjectApplicationRepository.findByProject_ProjectId(projectId);
        return members.stream()
                .map(member -> ProjectApplicationResponseDTO.builder()
                        .id(member.getId())
                        .userId(member.getUser().getUserId())
//                        .userName(member.getUser().getName())  * 이름 필드 삭제
                        .techPart(member.getTechPart())
                        .applicationMessage(member.getApplicationMessage())
                        .status(member.getStatus().name())
                        .build())
                .toList();
    }

    // 지원자 상태 업데이트 (승인 시 프로젝트 멤버 자동 편입)
    @Transactional
    public void updateMemberStatus(Long projectId, Long memberId, Long teamLeaderId, String status) {
        // 1) 프로젝트 조회
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

        // 2) 팀장 검증
        if (!Objects.equals(project.getTeamLeader().getUserId(), teamLeaderId)) {
            throw new AccessDeniedException("팀장만 승인/거절이 가능합니다.");
        }

        // 3) 지원자 조회
        ProjectApplication applicant = ProjectApplicationRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("지원자를 찾을 수 없습니다."));

        // 4) 해당 프로젝트 소속 지원자인지 검증
        if (!Objects.equals(applicant.getProject().getProjectId(), projectId)) {
            throw new AccessDeniedException("잘못된 접근입니다.");
        }

        // 5) 상태 변경
        if ("ACCEPTED".equalsIgnoreCase(status)) {
            applicant.setStatus(ProjectApplication.ApplicationStatus.ACCEPTED);

            // 승인 시 프로젝트 멤버 자동 편입 (중복 방지 + 동시성 안전)
            Long userId = applicant.getUser().getUserId();
            if (!projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(projectId, userId)) {
                try {
                    projectMemberRepository.save(ProjectMember.builder()
                            .project(project)
                            .user(applicant.getUser())
                            .role(ProjectMember.Role.MEMBER)
                            .techPart(applicant.getTechPart()) // 어떤 파트로 합류했는지 기록
                            .build());
                } catch (DataIntegrityViolationException ignore) {
                    // 동시에 두 번 승인 눌려도 unique 제약에 의해 한 번만 들어가도록 무시
                }
            }

        } else if ("REJECTED".equalsIgnoreCase(status)) {
            applicant.setStatus(ProjectApplication.ApplicationStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다.");
        }
    }
}
