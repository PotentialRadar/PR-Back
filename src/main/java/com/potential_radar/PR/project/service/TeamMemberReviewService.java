package com.potential_radar.PR.project.service;

import com.potential_radar.PR.project.dto.TeamMemberReviewRequestDto;
import com.potential_radar.PR.project.repository.ProjectMemberRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.project.repository.TeamMemberReviewRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.user.service.UserReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.potential_radar.PR.common.exception.AccessDeniedException;
import com.potential_radar.PR.common.exception.NotFoundException;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectStatus;
import com.potential_radar.PR.project.domain.TeamMemberReview;
import com.potential_radar.PR.user.domain.User;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamMemberReviewService {

    private final TeamMemberReviewRepository teamMemberReviewRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final UserReputationService userReputationService;

    public void createReview(TeamMemberReviewRequestDto request, Long reviewerId) {
        // 1. 프로젝트 조회
        ProjectRecruitment project = projectRecruitmentRepository.findById(request.getProjectId())
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

        // 2. 리뷰 작성 가능 조건 확인 (OR 조건)
        boolean isCompletedByStatus = (project.getStatus() == ProjectStatus.COMPLETED);
        boolean isCompletedByDate = false;
        if (project.getEndDate() != null) {
            isCompletedByDate = java.time.LocalDate.now().isAfter(project.getEndDate().plusDays(3));
        }

        if (!isCompletedByStatus && !isCompletedByDate) {
            throw new IllegalStateException("프로젝트가 완료되지 않았거나, 종료일로부터 3일이 지나지 않아 리뷰를 작성할 수 없습니다.");
        }

        // 4. 자기 자신을 리뷰하는지 확인 (셀프 리뷰 방지)
        if (reviewerId.equals(request.getRevieweeId())) {
            throw new IllegalArgumentException("자기 자신을 리뷰할 수 없습니다.");
        }

        // 5. 리뷰 작성자(reviewer)가 실제 프로젝트 멤버인지 확인
        projectMemberRepository.findByProject_ProjectIdAndUser_UserId(project.getProjectId(), reviewerId)
                .orElseThrow(() -> new AccessDeniedException("해당 프로젝트의 멤버가 아니므로 리뷰를 작성할 권한이 없습니다."));

        // 6. 리뷰를 받으려는 사람(reviewee) 조회 및 프로젝트 멤버인지 확인
        User reviewee = userRepository.findById(request.getRevieweeId())
                .orElseThrow(() -> new NotFoundException("리뷰를 받으려는 사용자를 찾을 수 없습니다."));
        projectMemberRepository.findByProject_ProjectIdAndUser_UserId(project.getProjectId(), reviewee.getUserId())
                .orElseThrow(() -> new AccessDeniedException("리뷰를 받으려는 사용자가 해당 프로젝트의 멤버가 아닙니다."));

        // Get reviewer User object
        User reviewerUser = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("리뷰 작성자 정보를 찾을 수 없습니다."));

        // 7. 중복 리뷰 방지 (동일 프로젝트, 동일 작성자, 동일 리뷰 대상)
        if (teamMemberReviewRepository.existsByProjectAndReviewerAndReviewee(project, reviewerUser, reviewee)) {
            throw new IllegalArgumentException("이미 해당 팀원에 대한 리뷰를 작성했습니다.");
        }

        // 8. TeamMemberReview 엔티티를 생성하고 저장
        TeamMemberReview review = TeamMemberReview.builder()
                .project(project)
                .reviewer(reviewerUser) // Add reviewer
                .reviewee(reviewee)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        teamMemberReviewRepository.save(review);
        
        // 9. 📊 리뷰 대상자의 평판 점수 업데이트
        userReputationService.updateUserReputation(reviewee.getUserId());
    }
}
