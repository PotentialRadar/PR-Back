package com.potential_radar.PR.project.service;

import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.dto.ProjectApplyRequest;
import com.potential_radar.PR.project.dto.ProjectMemberResponseDTO;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final UserRepository userRepository;

    // 프로젝트 지원
    @Transactional public void applyProject(Long projectId, ProjectApplyRequest request) {
        if (projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(projectId, request.getUserId())) {
            throw new IllegalArgumentException("이미 지원하였습니다.");
        }
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .status(ProjectMember.MemberStatus.APPLIED)
                .techStack(request.getTechStack())
                .applicationMessage(request.getApplicationMessage())
                .build();
        projectMemberRepository.save(member);

    }

    // 프로젝트 지원자 목록
    public List<ProjectMemberResponseDTO> getProjectMembers(Long projectId) {
        List<ProjectMember> members = projectMemberRepository.findByProject_ProjectId(projectId);
        return members.stream()
                .map(member -> ProjectMemberResponseDTO.builder()
                        .id(member.getId())
                        .userId(member.getUser().getUserId())
                        .userName(member.getUser().getName()) // 필요하다면
                        .techStack(member.getTechStack())      // 엔티티에 필드 있으면
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
            throw new IllegalArgumentException("팀장만 승인/거절이 가능합니다.");
        }

        // 3. 지원자 찾기
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("지원자를 찾을 수 없습니다."));

        // 4. 지원자가 해당 프로젝트 소속인지 검증 (보안)
        if (!member.getProject().getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("잘못된 접근입니다.");
        }

        // 5. 상태 변경
        if ("ACCEPTED".equalsIgnoreCase(status)) {
            member.setStatus(ProjectMember.MemberStatus.ACCEPTED);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            member.setStatus(ProjectMember.MemberStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다.");
        }
        // 저장 생략: JPA dirty checking
    }
}
