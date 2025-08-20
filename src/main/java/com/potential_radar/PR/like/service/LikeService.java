package com.potential_radar.PR.like.service;

import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.dto.LikeRequestDto;
import com.potential_radar.PR.like.dto.LikeResponseDto;
import com.potential_radar.PR.like.repository.LikeRepository;
import com.potential_radar.PR.common.excetpion.NotFoundException;
import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.like.dto.LikeRequestDto;
import com.potential_radar.PR.like.dto.LikeResponseDto;
import com.potential_radar.PR.like.repository.LikeRepository;
import com.potential_radar.PR.project.domain.ProjectApplication;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectTechPart;
import com.potential_radar.PR.project.domain.ProjectTechStack;
import com.potential_radar.PR.project.dto.ProjectPartRecruitmentDTO;
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse;
import com.potential_radar.PR.project.dto.ProjectTechStackDTO;
import com.potential_radar.PR.project.repository.ProjectApplicationRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;

    //좋아요 클릭
    @Transactional
    public LikeResponseDto toggleLike(LikeRequestDto requestDto, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        likeRepository.findByUserAndTargetTypeAndTargetId(
                user, requestDto.getTargetType(), requestDto.getTargetId()
        ).ifPresentOrElse(
                likeRepository::delete,
                () -> {
                    Like like = new Like(user, requestDto.getTargetType(), requestDto.getTargetId());
                    likeRepository.save(like);
                }
        );

        long likeCount = likeRepository.countByTargetTypeAndTargetId(requestDto.getTargetType(), requestDto.getTargetId());
        boolean isLiked = likeRepository.existsByUserAndTargetTypeAndTargetId(user, requestDto.getTargetType(), requestDto.getTargetId());

        return new LikeResponseDto(likeCount, isLiked);
    }

    //프로젝트 좋아요 조회
    @Transactional(readOnly = true)
    public List<ProjectRecruitmentResponse> getLikedProjects(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        List<Like> likes = likeRepository.findAllByUserAndTargetType(user, TargetType.PROJECT);
        if (likes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> projectIds = likes.stream()
                .map(Like::getTargetId)
                .collect(Collectors.toList());

        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAllById(projectIds);
        List<ProjectRecruitmentResponse> responses = new ArrayList<>();

        for (ProjectRecruitment pr : projects) {
            List<ProjectTechStackDTO> techStackDTOs = pr.getTechStacks().stream()
                    .map(ts -> ProjectTechStackDTO.builder()
                            .techStackName(ts.getTechStackName())
                            .recruitCount(ts.getRecruitCount())
                            .build())
                    .collect(Collectors.toList());

            List<ProjectPartRecruitmentDTO> partDTOs = pr.getTechParts().stream()
                    .map(pt -> ProjectPartRecruitmentDTO.builder()
                            .partName(pt.getPartName())
                            .recruitCount(pt.getRecruitCount())
                            .build())
                    .collect(Collectors.toList());

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
}
