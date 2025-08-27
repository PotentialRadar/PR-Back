package com.potential_radar.PR.invitation.repository;

import com.potential_radar.PR.invitation.domain.InvitationStatus;
import com.potential_radar.PR.invitation.domain.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    
    // 특정 사용자가 받은 초대 목록 (최신순)
    List<TeamInvitation> findByInviteeUserIdOrderByCreatedAtDesc(Long inviteeUserId);
    
    // 특정 사용자가 보낸 초대 목록 (최신순)
    List<TeamInvitation> findByInviterUserIdOrderByCreatedAtDesc(Long inviterUserId);
    
    // N+1 문제 해결을 위한 fetch join 쿼리들
    @Query("SELECT ti FROM TeamInvitation ti " +
           "JOIN FETCH ti.inviter " +
           "JOIN FETCH ti.invitee " +
           "JOIN FETCH ti.project " +
           "WHERE ti.invitee.userId = :inviteeUserId " +
           "ORDER BY ti.createdAt DESC")
    List<TeamInvitation> findByInviteeUserIdWithDetailsOrderByCreatedAtDesc(@Param("inviteeUserId") Long inviteeUserId);
    
    @Query("SELECT ti FROM TeamInvitation ti " +
           "JOIN FETCH ti.inviter " +
           "JOIN FETCH ti.invitee " +
           "JOIN FETCH ti.project " +
           "WHERE ti.inviter.userId = :inviterUserId " +
           "ORDER BY ti.createdAt DESC")
    List<TeamInvitation> findByInviterUserIdWithDetailsOrderByCreatedAtDesc(@Param("inviterUserId") Long inviterUserId);
    
    // 특정 프로젝트의 초대 목록
    List<TeamInvitation> findByProjectProjectIdOrderByCreatedAtDesc(Long projectId);
    
    // 특정 상태의 초대 목록 조회
    List<TeamInvitation> findByInviteeUserIdAndStatusOrderByCreatedAtDesc(Long inviteeUserId, InvitationStatus status);
    
    // 중복 초대 확인 (같은 프로젝트에서 같은 사용자에게 대기중인 초대가 있는지)
    Optional<TeamInvitation> findByProjectProjectIdAndInviteeUserIdAndStatus(Long projectId, Long inviteeUserId, InvitationStatus status);
    
    // 프로젝트 오너가 보낸 초대 개수
    @Query("SELECT COUNT(ti) FROM TeamInvitation ti WHERE ti.inviter.userId = :inviterUserId AND ti.project.projectId = :projectId")
    long countByInviterUserIdAndProjectProjectId(@Param("inviterUserId") Long inviterUserId, @Param("projectId") Long projectId);
}