package com.potential_radar.PR.project.service;

import com.potential_radar.PR.common.domain.TechPart;
import com.potential_radar.PR.common.domain.TechStack;
import com.potential_radar.PR.common.exception.NotFoundException;
import com.potential_radar.PR.common.repository.TechPartRepository;
import com.potential_radar.PR.common.repository.TechStackRepository;

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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectRecruitmentService {
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTechPartRepository projectTechPartRepository;
    private final ProjectTechStackRepository projectTechStackRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProjectCommentRepository projectCommentRepository;
    private final TechStackRepository techStackRepository; // New
    private final TechPartRepository techPartRepository;   // New

    public ProjectRecruitmentResponse convertToResponseDto(ProjectRecruitment pr) {
        // 스택
        List<ProjectTechStackDTO> techStackDTOs = new ArrayList<>();
        for (ProjectTechStack ts : pr.getTechStacks()) {
            techStackDTOs.add(ProjectTechStackDTO.builder()
                    .techStackName(ts.getTechStack().getName())
                    .recruitCount(ts.getRecruitCount())
                    .build());
        }
        // 파트
        List<ProjectPartRecruitmentDTO> partDTOs = new ArrayList<>();
        for (ProjectTechPart pt : pr.getTechParts()) {
            partDTOs.add(ProjectPartRecruitmentDTO.builder()
                    .partName(pt.getTechPart().getName())
                    .recruitCount(pt.getRecruitCount())
                    .build());
        }

        int appliedCount = projectApplicationRepository.countByProject_ProjectId(pr.getProjectId());
        int acceptedCount = projectApplicationRepository.countByProject_ProjectIdAndStatus(
                pr.getProjectId(), ProjectApplication.ApplicationStatus.ACCEPTED);
        int remainingCount = pr.getRecruitCount() - acceptedCount;

        return ProjectRecruitmentResponse.builder()
                .projectId(pr.getProjectId())
                .teamLeaderId(pr.getTeamLeader().getUserId())
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
                .recruitmentParts(partDTOs)
                .build();
    }

    // 구인글 생성
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
                .recruitCount(request.getRecruitCount() != null ? request.getRecruitCount() : 0)
                .status(ProjectStatus.RECRUITING)
                .build();

        // 기술스택 연관 저장
        List<ProjectTechStack> techStacks = new ArrayList<>();
        if (request.getTechStacks() != null) {
            for (ProjectTechStackDTO tsDto : request.getTechStacks()) {
                String in = tsDto.getTechStackName() == null ? "" : tsDto.getTechStackName().trim();
                if (in.isEmpty()) throw new IllegalArgumentException("techStackName is required");

                Integer cnt = tsDto.getRecruitCount();
                if (cnt == null || cnt < 0) cnt = 0;

                TechStack foundTechStack = techStackRepository
                        .findByNameIgnoreCase(in) // 핵심
                        .orElseThrow(() -> new NotFoundException("TechStack not found: " + in));

                techStacks.add(ProjectTechStack.builder()
                        .project(project)
                        .techStack(foundTechStack)
                        .recruitCount(cnt)
                        .build());
            }
        }
        project.setTechStacks(techStacks);

        // 기술 파트 연관 저장
        List<ProjectTechPart> techParts = new ArrayList<>();
        if (request.getRecruitmentParts() != null) {
            Set<String> seen = new HashSet<>();
            for (ProjectPartRecruitmentDTO dto : request.getRecruitmentParts()) {
                String raw = dto.getPartName() == null ? "" : dto.getPartName().trim();
                String key = raw.toUpperCase(Locale.ROOT);

                if (raw.isEmpty()) throw new IllegalArgumentException("partName is required");
                if (dto.getRecruitCount() == null || dto.getRecruitCount() < 0)
                    throw new IllegalArgumentException("recruitCount must be >= 0");
                if (!seen.add(key)) throw new IllegalArgumentException("Duplicate partName: " + raw);

                TechPart foundTechPart = techPartRepository
                        .findByNameIgnoreCase(raw) // ✅ 핵심
                        .orElseThrow(() -> new NotFoundException("TechPart not found: " + raw));

                techParts.add(ProjectTechPart.builder()
                        .project(project)
                        .techPart(foundTechPart)
                        .recruitCount(dto.getRecruitCount())
                        .build());
            }
        }
        project.setTechParts(techParts);

        projectRecruitmentRepository.save(project); // cascade로 하위 엔티티도 저장

        // 🔥 프로젝트 생성 이벤트 발행
        eventPublisher.publishEvent(new ProjectCreatedEvent(project));

        // 팀리더 멤버 보장
        if (!projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(project.getProjectId(), teamLeader.getUserId())) {
            projectMemberRepository.save(ProjectMember.builder()
                    .project(project)
                    .user(teamLeader)
                    .role(ProjectMember.Role.LEADER)
                    .techPart(null) // 리더는 파트 없어도 OK
                    .build());
        }

        return project.getProjectId();
    }

    //구인글 조회
    @Transactional
    public ProjectRecruitmentResponse getProject(Long id) {
        ProjectRecruitment pr = projectRecruitmentRepository.findByIdWithTeamLeader(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));
        pr.setViewCount(pr.getViewCount() == null ? 1 : pr.getViewCount() + 1);

        // 스택 → DTO
        List<ProjectTechStackDTO> techStackDTOs = new ArrayList<>();
        for (ProjectTechStack ts : pr.getTechStacks()) {
            techStackDTOs.add(ProjectTechStackDTO.builder()
                    .techStackName(ts.getTechStack().getName()) // Get name from TechStack entity
                    .recruitCount(ts.getRecruitCount())
                    .build());
        }

        // 파트 → DTO
        List<ProjectPartRecruitmentDTO> partDTOs = new ArrayList<>();
        for (ProjectTechPart pt : pr.getTechParts()) {
            partDTOs.add(ProjectPartRecruitmentDTO.builder()
                    .partName(pt.getTechPart().getName()) // Get name from TechPart entity
                    .recruitCount(pt.getRecruitCount())
                    .build());
        }

        int appliedCount = projectApplicationRepository.countByProject_ProjectId(pr.getProjectId());
        int acceptedCount = projectApplicationRepository.countByProject_ProjectIdAndStatus(
                pr.getProjectId(), ProjectApplication.ApplicationStatus.ACCEPTED);
        int remainingCount = pr.getRecruitCount() - acceptedCount;

        return ProjectRecruitmentResponse.builder()
                .projectId(pr.getProjectId())
                .teamLeaderId(pr.getTeamLeader().getUserId())
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
                .recruitmentParts(partDTOs)
                .build();
    }

    // 전체 구인글 목록 조회
    @Transactional(readOnly = true)
    public List<ProjectRecruitmentResponse> getAllProjects() {
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAll();
        return projects.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // 사용자가 생성한 프로젝트 목록 조회
    @Transactional(readOnly = true)
    public List<ProjectRecruitmentResponse> getProjectsCreatedByUser(Long userId) {
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findByTeamLeader_UserId(userId);
        return projects.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // 구인글 수정 (전량 삭제 → 재삽입)
    @Transactional
    public void updateProject(Long id, ProjectRecruitmentRequest request) {
        ProjectRecruitment project = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));

        // 이전 상태 저장 (상태 변경 이벤트를 위해)
        ProjectStatus previousStatus = project.getStatus();

        // 1) 기본 필드 업데이트
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setRecruitDeadline(request.getRecruitDeadline());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setFileUrl(request.getFileUrl());
        if (request.getStatus() != null) project.setStatus(ProjectStatus.valueOf(request.getStatus()));
        if (request.getRecruitCount() != null) project.setRecruitCount(request.getRecruitCount());

        // 2) 자식 전량 삭제 (벌크)
        projectTechStackRepository.deleteAllByProjectId(id);
        projectTechPartRepository.deleteAllByProjectId(id);

        // (선택) DB에 삭제를 먼저 확정하고 싶다면 중간 flush
        // em.flush();

        // 3) 요청 바탕으로 자식 재삽입
        // 3-1) TechStacks
        if (request.getTechStacks() != null && !request.getTechStacks().isEmpty()) {
            for (ProjectTechStackDTO tsDto : request.getTechStacks()) {
                if (tsDto.getTechStackName() == null || tsDto.getTechStackName().isBlank()) {
                    throw new IllegalArgumentException("techStackName is required");
                }
                Integer cnt = tsDto.getRecruitCount();
                if (cnt != null && cnt < 0) throw new IllegalArgumentException("techStack.recruitCount must be >= 0");

                TechStack foundTechStack = techStackRepository.findByNameIgnoreCase(tsDto.getTechStackName().trim())
                        .orElseThrow(() -> new NotFoundException("TechStack not found: " + tsDto.getTechStackName()));

                ProjectTechStack stack = ProjectTechStack.builder()
                        .project(project)
                        .techStack(foundTechStack) // Use the found TechStack entity
                        .recruitCount(cnt)
                        .build();

                projectTechStackRepository.save(stack);
            }
        }

        // 3-2) TechParts (중복 방지 검증 포함)
        if (request.getRecruitmentParts() != null && !request.getRecruitmentParts().isEmpty()) {
            Set<String> seen = new HashSet<>();
            for (ProjectPartRecruitmentDTO dto : request.getRecruitmentParts()) {
                String name = (dto.getPartName() == null ? "" : dto.getPartName().trim()).toUpperCase(Locale.ROOT);
                if (name.isEmpty()) throw new IllegalArgumentException("partName is required");
                Integer cnt = dto.getRecruitCount();
                if (cnt == null || cnt < 0) throw new IllegalArgumentException("recruitCount must be >= 0");
                if (!seen.add(name)) throw new IllegalArgumentException("Duplicate partName: " + name);

                TechPart foundTechPart = techPartRepository.findByNameIgnoreCase(name)
                        .orElseThrow(() -> new NotFoundException("TechPart not found: " + name));

                ProjectTechPart part = ProjectTechPart.builder()
                        .project(project)
                        .techPart(foundTechPart) // Use the found TechPart entity
                        .recruitCount(cnt)
                        .build();

                projectTechPartRepository.save(part);
            }
        }
    }
    // 구인글 삭제
    @Transactional
    public void deleteProject(Long id) {
        ProjectRecruitment project = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));

        // 1) 자식 테이블 전부 삭제 (순서는 크게 상관없지만, 일관성을 위해 통일)
        projectApplicationRepository.deleteAllByProjectId(id);
        projectMemberRepository.deleteAllByProjectId(id);
        projectTechStackRepository.deleteAllByProjectId(id);
        projectTechPartRepository.deleteAllByProjectId(id);
        projectCommentRepository.deleteAllByProjectId(id);

        // 2) 부모 삭제
        projectRecruitmentRepository.delete(project);

        // 🔥 프로젝트 삭제 이벤트 발행
        eventPublisher.publishEvent(new ProjectDeletedEvent(id, projectName));
    }
}
