package com.potential_radar.PR.project.service;

import com.potential_radar.PR.project.domain.ProjectMember;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.repository.ProjectMemberRespository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {
    private final ProjectMemberRespository projectMemberRespository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;

    // 지원자 등록
    @Transactional
    public Long applyToProject(Long projectId, Long userId) {
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 중복 지원 방지
        if (projectMemberRepository.existsByProject_ProjectIdAndUser_Id(projectId, userId)) {
            throw new IllegalArgumentException("이미 지원한 프로젝트입니다.");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .status(ProjectMember.MemberStatus.APPLIED)
                .build();

        return projectMemberRepository.save(member).getId();
    }
}
