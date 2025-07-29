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
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTechStackRepository projectTechStackRepository;

    // 전체 프로젝트 리스트 반환
    @Transactional(readOnly = true)
    public List<ProjectListResponse> getProjectList() {
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAll();
        List<ProjectListResponse> result = new ArrayList<>();

        for (ProjectRecruitment pr : projects) {
            // 지원자 수 구하기
            int appliedCount = projectMemberRepository.countByProject_ProjectId(pr.getProjectId());

            // 기술스택 String 리스트로 가공
            List<String> techStackNames = pr.getTechStacks().stream()
                    .map(ProjectTechStack::getTechStackName)
                    .toList();

            result.add(ProjectListResponse.builder()
                    .projectId(pr.getProjectId())
                    .title(pr.getTitle())
                    .description(pr.getDescription())
                    .status(pr.getStatus().name())    // 예: "RECRUITING"
                    .recruitCount(pr.getRecruitCount())
                    .appliedCount(appliedCount)
                    .recruitDeadline(pr.getRecruitDeadline())
                    .techStacks(techStackNames)
                    .build()
            );
        }

        return result;
    }

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
                .recruitCount(request.getRecruitCount())
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

        projectRecruitmentRepository.save(project); // Cascade로 techStacks도 같이 저장됨
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
                            .techStackName(ts.getTechStackName())
                            .recruitCount(ts.getRecruitCount())
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
                        .techStackName(tsDto.getTechStackName())
                        .recruitCount(tsDto.getRecruitCount())
                        .build();
                newStacks.add(techStack);
            }
        }
        project.getTechStacks().addAll(newStacks);
    }

    // 구인글 삭제
    @Transactional
    public void deleteProject(Long id) {
        ProjectRecruitment project = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 구인글이 존재하지 않습니다."));
        projectRecruitmentRepository.delete(project);
    }

}
