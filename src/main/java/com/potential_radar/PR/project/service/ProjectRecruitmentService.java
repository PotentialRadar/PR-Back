package com.potential_radar.PR.project.service;


import com.potential_radar.PR.common.exception.AccessDeniedException;
import com.potential_radar.PR.common.exception.NotFoundException;

import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.like.repository.LikeRepository;
import com.potential_radar.PR.like.service.LikeService;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.dto.*;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.tech.domain.TechPart;
import com.potential_radar.PR.tech.domain.TechStack;
import com.potential_radar.PR.tech.repository.TechPartRepository;
import com.potential_radar.PR.tech.repository.TechStackRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ProjectRecruitmentService {
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTechPartRepository projectTechPartRepository;
    private final ProjectTechStackRepository projectTechStackRepository;
    private final ProjectCommentRepository projectCommentRepository;
    private final TechStackRepository techStackRepository;
    private final TechPartRepository techPartRepository;
    private final LikeService likeService;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;

    public ProjectRecruitmentResponse convertToResponseDto(ProjectRecruitment pr, String userEmail) {
        boolean isLiked = false;
        if (userEmail != null) {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                isLiked = likeRepository.existsByUserAndTargetTypeAndTargetId(userOpt.get(), TargetType.PROJECT, pr.getProjectId());
            }
        }

        List<ProjectTechStackDTO> techStackDTOs = pr.getTechStacks().stream()
                .map(ts -> ProjectTechStackDTO.builder()
                        .techStackName(ts.getTechStack().getName())
                        .recruitCount(ts.getRecruitCount())
                        .build())
                .collect(Collectors.toList());

        List<ProjectPartRecruitmentDTO> partDTOs = pr.getTechParts().stream()
                .map(pt -> ProjectPartRecruitmentDTO.builder()
                        .partName(pt.getTechPart().getName())
                        .recruitCount(pt.getRecruitCount())
                        .build())
                .collect(Collectors.toList());

        int appliedCount = projectApplicationRepository.countByProject_ProjectId(pr.getProjectId());
        int acceptedCount = projectApplicationRepository.countByProject_ProjectIdAndStatus(
                pr.getProjectId(), ProjectApplication.ApplicationStatus.ACCEPTED);
        int remainingCount = pr.getRecruitCount() - acceptedCount;
        long likeCount = likeService.getLikeCount(TargetType.PROJECT, pr.getProjectId());

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
                .likeCount(likeCount)
                .isLiked(isLiked)
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
                        .findByNameIgnoreCase(in)
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
                        .findByNameIgnoreCase(raw)
                        .orElseThrow(() -> new NotFoundException("TechPart not found: " + raw));

                techParts.add(ProjectTechPart.builder()
                        .project(project)
                        .techPart(foundTechPart)
                        .recruitCount(dto.getRecruitCount())
                        .build());
            }
        }
        project.setTechParts(techParts);

        projectRecruitmentRepository.save(project);

        if (!projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(project.getProjectId(), teamLeader.getUserId())) {
            projectMemberRepository.save(ProjectMember.builder()
                    .project(project)
                    .user(teamLeader)
                    .role(ProjectMember.Role.LEADER)
                    .techPart(null)
                    .build());
        }

        return project.getProjectId();
    }

    @Transactional
    public ProjectRecruitmentResponse getProject(Long id, String userEmail) {
        ProjectRecruitment pr = projectRecruitmentRepository.findByIdWithTeamLeader(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));
        pr.setViewCount(pr.getViewCount() == null ? 1 : pr.getViewCount() + 1);
        return convertToResponseDto(pr, userEmail);
    }

    @Transactional(readOnly = true)
    public Page<ProjectRecruitmentResponse> getAllProjects(String userEmail, Pageable pageable) {
        Page<ProjectRecruitment> projects = projectRecruitmentRepository.findAll(pageable);
        return projects.map(pr -> convertToResponseDto(pr, userEmail));
    }

    @Transactional(readOnly = true)
    public List<ProjectRecruitmentResponse> getProjectsCreatedByUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findByTeamLeader_UserId(userId);
        return projects.stream()
                .map(pr -> convertToResponseDto(pr, user.getEmail()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponseDTO> getConfirmedProjectMembers(Long projectId, Long currentUserId) {
        projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

        projectMemberRepository.findByProject_ProjectIdAndUser_UserId(projectId, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("해당 프로젝트의 멤버만 팀원 목록을 볼 수 있습니다."));

        List<ProjectMember> members = projectMemberRepository.findAllByProject_ProjectId(projectId);

        return members.stream()
                .map(member -> ProjectMemberResponseDTO.builder()
                        .userId(member.getUser().getUserId())
                        .userName(member.getUser().getNickname())
                        .role(member.getRole().name())
                        .techPart(member.getTechPart())
                        .build())
                .collect(Collectors.toList());
    }

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
        if (request.getStatus() != null) project.setStatus(ProjectStatus.valueOf(request.getStatus()));
        if (request.getRecruitCount() != null) project.setRecruitCount(request.getRecruitCount());

        projectTechStackRepository.deleteAllByProjectId(id);
        projectTechPartRepository.deleteAllByProjectId(id);

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
                        .techPart(foundTechPart)
                        .recruitCount(cnt)
                        .build();

                projectTechPartRepository.save(part);
            }
        }
    }

    // 구인글 상태 수정
    @Transactional
    public void updateProjectStatus(Long projectId, String status, Long userId) {
        ProjectRecruitment project = projectRecruitmentRepository.findByIdWithTeamLeader(projectId)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));

        // 팀 리더 권한 확인
        if (!project.getTeamLeader().getUserId().equals(userId)) {
            throw new AccessDeniedException("프로젝트 상태를 변경할 권한이 없습니다.");
        }

        try {
            ProjectStatus newStatus = ProjectStatus.valueOf(status.toUpperCase());
            project.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다: " + status);
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
    }
}
