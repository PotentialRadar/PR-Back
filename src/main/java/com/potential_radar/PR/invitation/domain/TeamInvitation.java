package com.potential_radar.PR.invitation.domain;

import com.potential_radar.PR.common.domain.BaseTimeEntity;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamInvitation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectRecruitment project; // 초대하는 프로젝트

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter; // 초대를 보내는 사람 (프로젝트 오너)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee; // 초대받는 사람

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "message", length = 500)
    private String message; // 초대 메시지

    @Column(name = "responded_at")
    private LocalDateTime respondedAt; // 응답 시간

    @Builder
    public TeamInvitation(ProjectRecruitment project, User inviter, User invitee, String message) {
        this.project = project;
        this.inviter = inviter;
        this.invitee = invitee;
        this.message = message;
        this.status = InvitationStatus.PENDING;
    }

    // 초대 수락
    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    // 초대 거절
    public void reject() {
        this.status = InvitationStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    // 초대가 아직 대기중인지 확인
    public boolean isPending() {
        return this.status == InvitationStatus.PENDING;
    }
}