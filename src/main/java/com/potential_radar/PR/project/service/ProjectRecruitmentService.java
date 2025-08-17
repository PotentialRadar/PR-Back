package com.potential_radar.PR.project.service;

import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.dto.*;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.search.event.ProjectCreatedEvent;
import com.potential_radar.PR.search.event.ProjectUpdatedEvent;
import com.potential_radar.PR.search.event.ProjectDeletedEvent;
import com.potential_radar.PR.search.event.ProjectStatusChangedEvent;
import com.potential_radar.PR.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectRecruitmentService {
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTechStackRepository projectTechStackRepository;
    private final ApplicationEventPublisher eventPublisher;

    //구인글 생성
    @Transactional
    public Long createProject(ProjectRecruitmentRequest request, User teamLeader) {
        ProjectRecruitment project = ProjectRecruitment.builder()
                .title(request.getTitle())
                .teamLeader(teamLeader)
                .description(request.getDescription())
                .recruitDeadline(request.getRecruitDeadline())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .fileUrl(request.getFileUrl())
                .status(ProjectStatus.RECRUITING)
                .build();

        // 기술스택 연관 저장
        List<ProjectTechStack> techStacks = new ArrayList<>();
        if (request.getTechStacks() != null) {
            for (ProjectTechStackDTO tsDto : request.getTechStacks()) {
                ProjectTechStack techStack = ProjectTechStack.builder()
                        .project(project)
                        .build();
                techStacks.add(techStack);
            }
        }
        project.setTechStacks(techStacks);

        projectRecruitmentRepository.save(project); // Cascade로 techStacks도 같이 저장됨
        
        // 🔥 프로젝트 생성 이벤트 발행
        eventPublisher.publishEvent(new ProjectCreatedEvent(project));
        
        return project.getProjectId();
    }

    //구인글 조회
    @Transactional
    public ProjectRecruitmentResponse getProject(Long id) {
        ProjectRecruitment pr = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));
        pr.setViewCount(pr.getViewCount() == null ? 1 : pr.getViewCount() + 1);
        List<ProjectTechStackDTO> techStackDTOs = new ArrayList<>();
        for (ProjectTechStack ts : pr.getTechStacks()) {
            techStackDTOs.add(
                    ProjectTechStackDTO.builder()
                            .build()
            );
        }
        // 전체 지원자 수
        int appliedCount = projectMemberRepository.countByProject_ProjectId(pr.getProjectId());

        // 승인된 지원자 수
        int acceptedCount = projectMemberRepository.countByProject_ProjectIdAndStatus(
                pr.getProjectId(), ProjectMember.MemberStatus.ACCEPTED);

        // 남은 자리 (모집인원 - 승인된 지원자)
        int remainingCount = pr.getRecruitCount() - acceptedCount;

        return ProjectRecruitmentResponse.builder()
                .projectId(pr.getProjectId())
                .title(pr.getTitle())
                .description(pr.getDescription())
                .recruitDeadline(pr.getRecruitDeadline())
                .startDate(pr.getStartDate())
                .endDate(pr.getEndDate())
                .fileUrl(pr.getFileUrl())
                .status(pr.getStatus().name())
                .viewCount(pr.getViewCount())
                .recruitCount(pr.getRecruitCount())
                .appliedCount(appliedCount)
                .acceptedCount(acceptedCount)
                .remainingCount(remainingCount)
                .techStacks(techStackDTOs)
                .build();
    }

    // 구인글 수정
    @Transactional
    public void updateProject(Long id, ProjectRecruitmentRequest request) {
        ProjectRecruitment project = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));

        // 이전 상태 저장 (상태 변경 이벤트를 위해)
        ProjectStatus previousStatus = project.getStatus();

        // 필드 업데이트
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setRecruitDeadline(request.getRecruitDeadline());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setFileUrl(request.getFileUrl());

        if (request.getStatus() != null) {
            project.setStatus(ProjectStatus.valueOf(request.getStatus()));
        }

        // 기술스택 업데이트 (기존 스택 모두 삭제 후 새로 추가)
        project.getTechStacks().clear();
        List<ProjectTechStack> newStacks = new ArrayList<>();
        if (request.getTechStacks() != null) {
            for (ProjectTechStackDTO tsDto : request.getTechStacks()) {
                ProjectTechStack techStack = ProjectTechStack.builder()
                        .project(project)
                        .build();
                newStacks.add(techStack);
            }
        }
        project.getTechStacks().addAll(newStacks);
        
        // 🔥 프로젝트 업데이트 이벤트 발행
        eventPublisher.publishEvent(new ProjectUpdatedEvent(project));
        
        // 🔥 상태가 변경된 경우 상태 변경 이벤트 발행
        if (previousStatus != project.getStatus()) {
            eventPublisher.publishEvent(new ProjectStatusChangedEvent(project, previousStatus, project.getStatus()));
        }
    }

    // 구인글 삭제
    @Transactional
    public void deleteProject(Long id) {
        ProjectRecruitment project = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));
        
        // 삭제하기 전에 프로젝트 정보 저장 (이벤트를 위해)
        String projectName = project.getTitle();
        
        projectRecruitmentRepository.delete(project);
        
        // 🔥 프로젝트 삭제 이벤트 발행
        eventPublisher.publishEvent(new ProjectDeletedEvent(id, projectName));
    }

}
