package com.potential_radar.PR.project.repository;

import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.TeamMemberReview;
import com.potential_radar.PR.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamMemberReviewRepository extends JpaRepository<TeamMemberReview, Long> {
    boolean existsByProjectAndReviewerAndReviewee(ProjectRecruitment project, User reviewer, User reviewee);
    
    @Query("SELECT tmr FROM TeamMemberReview tmr JOIN FETCH tmr.reviewer JOIN FETCH tmr.project WHERE tmr.reviewee.userId = :revieweeId ORDER BY tmr.createdAt DESC")
    List<TeamMemberReview> findAllByRevieweeIdWithDetails(@Param("revieweeId") Long revieweeId);
}
