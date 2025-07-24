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
    @Transactional
    public void applyProject(Long projectId, ProjectApplyRequest request) {
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
}
