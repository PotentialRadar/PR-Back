package com.potential_radar.PR.project.service;

import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.dto.*;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectRecruitmentService {
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;

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
                ProjectTechStack techStack = ProjectTechStack.builder()
                        .project(project)
                        .techStackName(tsDto.getTechStackName())
                        .recruitCount(tsDto.getRecruitCount())
                        .build();
                techStacks.add(techStack);
            }
        }
        project.setTechStacks(techStacks);

        // 기술 파트 연관 저장
        List<ProjectTechPart> techParts = new ArrayList<>();
        if (request.getRecruitmentParts() != null) {
            Set<String> seen = new HashSet<>();
            for (ProjectPartRecruitmentDTO dto : request.getRecruitmentParts()) {
                String name = (dto.getPartName() == null ? "" : dto.getPartName().trim()).toUpperCase(Locale.ROOT);
                if (name.isEmpty()) throw new IllegalArgumentException("partName is required");
                if (dto.getRecruitCount() == null || dto.getRecruitCount() < 0)
                    throw new IllegalArgumentException("recruitCount must be >= 0");
                if (!seen.add(name)) throw new IllegalArgumentException("Duplicate partName: " + name);

                techParts.add(ProjectTechPart.builder()
                        .project(project)
                        .partName(name)
                        .recruitCount(dto.getRecruitCount())
                        .build());
            }
        }
        project.setTechParts(techParts);

        projectRecruitmentRepository.save(project); // cascade로 하위 엔티티도 저장

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
        ProjectRecruitment pr = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));
        pr.setViewCount(pr.getViewCount() == null ? 1 : pr.getViewCount() + 1);

        // 스택 → DTO
        List<ProjectTechStackDTO> techStackDTOs = new ArrayList<>();
        for (ProjectTechStack ts : pr.getTechStacks()) {
            techStackDTOs.add(ProjectTechStackDTO.builder()
                    .techStackName(ts.getTechStackName())
                    .recruitCount(ts.getRecruitCount())
                    .build());
        }

        // 파트 → DTO
        List<ProjectPartRecruitmentDTO> partDTOs = new ArrayList<>();
        for (ProjectTechPart pt : pr.getTechParts()) {
            partDTOs.add(ProjectPartRecruitmentDTO.builder()
                    .partName(pt.getPartName())
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
        List<ProjectRecruitmentResponse> responses = new ArrayList<>();

        for (ProjectRecruitment pr : projects) {
            // 스택
            List<ProjectTechStackDTO> techStackDTOs = new ArrayList<>();
            for (ProjectTechStack ts : pr.getTechStacks()) {
                techStackDTOs.add(ProjectTechStackDTO.builder()
                        .techStackName(ts.getTechStackName())
                        .recruitCount(ts.getRecruitCount())
                        .build());
            }
            // 파트
            List<ProjectPartRecruitmentDTO> partDTOs = new ArrayList<>();
            for (ProjectTechPart pt : pr.getTechParts()) {
                partDTOs.add(ProjectPartRecruitmentDTO.builder()
                        .partName(pt.getPartName())
                        .recruitCount(pt.getRecruitCount())
                        .build());
            }

            int appliedCount = projectApplicationRepository.countByProject_ProjectId(pr.getProjectId());
            int acceptedCount = projectApplicationRepository.countByProject_ProjectIdAndStatus(
                    pr.getProjectId(), ProjectApplication.ApplicationStatus.ACCEPTED);
            int remainingCount = pr.getRecruitCount() - acceptedCount;

            responses.add(ProjectRecruitmentResponse.builder()
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
                    .build());
        }
        return responses;
    }

    // 구인글 수정 (통째 교체)
    @Transactional
    public void updateProject(Long id, ProjectRecruitmentRequest request) {
        ProjectRecruitment project = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));

        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setRecruitDeadline(request.getRecruitDeadline());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setFileUrl(request.getFileUrl());

        if (request.getStatus() != null) {
            project.setStatus(ProjectStatus.valueOf(request.getStatus()));
        }
        if (request.getRecruitCount() != null) {
            project.setRecruitCount(request.getRecruitCount());
        }

        // 스택 교체
        project.getTechStacks().clear();
        List<ProjectTechStack> newStacks = new ArrayList<>();
        if (request.getTechStacks() != null) {
            for (ProjectTechStackDTO tsDto : request.getTechStacks()) {
                newStacks.add(ProjectTechStack.builder()
                        .project(project)
                        .techStackName(tsDto.getTechStackName())
                        .recruitCount(tsDto.getRecruitCount())
                        .build());
            }
        }
        project.getTechStacks().addAll(newStacks);

        // ✅ 파트 교체
        project.getTechParts().clear();
        List<ProjectTechPart> newParts = new ArrayList<>();
        if (request.getRecruitmentParts() != null) {
            Set<String> seen = new HashSet<>();
            for (ProjectPartRecruitmentDTO dto : request.getRecruitmentParts()) {
                String name = (dto.getPartName() == null ? "" : dto.getPartName().trim()).toUpperCase(Locale.ROOT);
                if (name.isEmpty()) throw new IllegalArgumentException("partName is required");
                if (dto.getRecruitCount() == null || dto.getRecruitCount() < 0)
                    throw new IllegalArgumentException("recruitCount must be >= 0");
                if (!seen.add(name)) throw new IllegalArgumentException("Duplicate partName: " + name);

                newParts.add(ProjectTechPart.builder()
                        .project(project)
                        .partName(name)
                        .recruitCount(dto.getRecruitCount())
                        .build());
            }
        }
        project.getTechParts().addAll(newParts);
    }

    // 구인글 삭제
    @Transactional
    public void deleteProject(Long id) {
        ProjectRecruitment project = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));
        projectRecruitmentRepository.delete(project);
    }
}