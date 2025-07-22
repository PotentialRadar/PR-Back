package com.potential_radar.PR.project.service;

import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.dto.*;
import com.potential_radar.PR.project.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectRecruitmentService {
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectTechStackRepository projectTechStackRepository;

    @Transactional
    public Long createProject(ProjectRecruitmentRequest request) {
        ProjectRecruitment project = ProjectRecruitment.builder()
                .title(request.getTitle())
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

    @Transactional
    public ProjectRecruitmentResponse getProject(Long id) {
        ProjectRecruitment pr = projectRecruitmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("구인글 없음"));
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
                .techStacks(techStackDTOs)
                .build();
    }

}
