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
    
    /**
     * 특정 사용자가 받은 리뷰들의 평균 점수와 총 개수를 계산
     * @param revieweeId 리뷰를 받은 사용자 ID
     * @return Object[] {평균점수(Double), 리뷰개수(Long)} 또는 null (리뷰가 없는 경우)
     */
    @Query("SELECT AVG(CAST(tmr.rating AS double)), COUNT(tmr) FROM TeamMemberReview tmr WHERE tmr.reviewee.userId = :revieweeId")
    Object[] calculateUserReviewStats(@Param("revieweeId") Long revieweeId);
}
