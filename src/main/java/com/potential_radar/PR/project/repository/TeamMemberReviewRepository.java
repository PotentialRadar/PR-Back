package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.TeamMemberReview;
import com.potential_radar.PR.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberReviewRepository extends JpaRepository<TeamMemberReview, Long> {
    boolean existsByProjectAndReviewerAndReviewee(ProjectRecruitment project, User reviewer, User reviewee);
}
