package com.potential_radar.PR.invitation.service;

import com.potential_radar.PR.common.exception.NotFoundException;
import com.potential_radar.PR.invitation.domain.InvitationStatus;
import com.potential_radar.PR.invitation.domain.TeamInvitation;
import com.potential_radar.PR.invitation.dto.TeamInvitationDto;
import com.potential_radar.PR.invitation.repository.TeamInvitationRepository;
import com.potential_radar.PR.notification.domain.NotificationType;
import com.potential_radar.PR.notification.service.NotificationService;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamInvitationService {

    private final TeamInvitationRepository teamInvitationRepository;
    private final UserRepository userRepository;
    private final ProjectRecruitmentRepository projectRepository;
    private final NotificationService notificationService;

    /**
     * 팀원 초대 보내기
     */
    @Transactional
    public TeamInvitationDto.ApiResponse sendInvitation(Long inviterId, TeamInvitationDto.SendRequest request) {
        try {
            // 1. 초대하는 사람 확인
            User inviter = userRepository.findById(inviterId)
                    .orElseThrow(() -> new NotFoundException("초대하는 사용자를 찾을 수 없습니다."));

            // 2. 초대받는 사람 확인
            User invitee = userRepository.findById(request.getInviteeId())
                    .orElseThrow(() -> new NotFoundException("초대받는 사용자를 찾을 수 없습니다."));

            // 3. 프로젝트 확인
            ProjectRecruitment project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

            // 4. 프로젝트 오너 권한 확인
            if (!project.getTeamLeader().getUserId().equals(inviterId)) {
                return TeamInvitationDto.ApiResponse.builder()
                        .success(false)
                        .message("프로젝트 오너만 초대를 보낼 수 있습니다.")
                        .build();
            }

            // 5. 중복 초대 확인
            Optional<TeamInvitation> existingInvitation = teamInvitationRepository
                    .findByProjectProjectIdAndInviteeUserIdAndStatus(request.getProjectId(), request.getInviteeId(), InvitationStatus.PENDING);
            
            if (existingInvitation.isPresent()) {
                return TeamInvitationDto.ApiResponse.builder()
                        .success(false)
                        .message("이미 해당 사용자에게 초대를 보냈습니다.")
                        .build();
            }

            // 6. 자기 자신에게 초대 방지
            if (inviterId.equals(request.getInviteeId())) {
                return TeamInvitationDto.ApiResponse.builder()
                        .success(false)
                        .message("자기 자신에게는 초대를 보낼 수 없습니다.")
                        .build();
            }

            // 7. 초대 생성 및 저장
            TeamInvitation invitation = TeamInvitation.builder()
                    .project(project)
                    .inviter(inviter)
                    .invitee(invitee)
                    .message(request.getMessage())
                    .build();

            TeamInvitation savedInvitation = teamInvitationRepository.save(invitation);

            // 8. 알림 전송
            try {
                String notificationContent = String.format("%s님이 '%s' 프로젝트에 초대했습니다.", 
                    inviter.getNickname(), project.getTitle());
                String notificationUrl = "/projects/" + project.getProjectId();
                
                notificationService.send(
                    invitee,
                    NotificationType.INVITATION,
                    notificationContent,
                    notificationUrl,
                    savedInvitation.getId(),
                    savedInvitation.getCreatedAt()
                );
                
                log.info("팀 초대 알림 전송 완료: {} → {}", inviter.getNickname(), invitee.getNickname());
            } catch (Exception e) {
                log.warn("팀 초대 알림 전송 실패: {}", e.getMessage());
            }

            log.info("팀원 초대 전송 완료: 프로젝트 {} → 사용자 {}", project.getTitle(), invitee.getNickname());

            return TeamInvitationDto.ApiResponse.builder()
                    .success(true)
                    .message("초대가 성공적으로 전송되었습니다.")
                    .data(convertToResponse(savedInvitation))
                    .build();

        } catch (Exception e) {
            log.error("팀원 초대 전송 실패: {}", e.getMessage(), e);
            return TeamInvitationDto.ApiResponse.builder()
                    .success(false)
                    .message("초대 전송에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 초대 응답하기 (수락/거절)
     */
    @Transactional
    public TeamInvitationDto.ApiResponse respondToInvitation(Long userId, TeamInvitationDto.ResponseRequest request) {
        try {
            // 1. 초대 조회
            TeamInvitation invitation = teamInvitationRepository.findById(request.getInvitationId())
                    .orElseThrow(() -> new NotFoundException("초대를 찾을 수 없습니다."));

            // 2. 권한 확인 (초대받은 본인만 응답 가능)
            if (!invitation.getInvitee().getUserId().equals(userId)) {
                return TeamInvitationDto.ApiResponse.builder()
                        .success(false)
                        .message("초대받은 본인만 응답할 수 있습니다.")
                        .build();
            }

            // 3. 이미 응답한 초대인지 확인
            if (!invitation.isPending()) {
                return TeamInvitationDto.ApiResponse.builder()
                        .success(false)
                        .message("이미 응답한 초대입니다.")
                        .build();
            }

            // 4. 응답 처리
            if (request.getStatus() == InvitationStatus.ACCEPTED) {
                invitation.accept();
                log.info("초대 수락: 사용자 {} → 프로젝트 {}", userId, invitation.getProject().getTitle());
            } else if (request.getStatus() == InvitationStatus.REJECTED) {
                invitation.reject();
                log.info("초대 거절: 사용자 {} → 프로젝트 {}", userId, invitation.getProject().getTitle());
            } else {
                return TeamInvitationDto.ApiResponse.builder()
                        .success(false)
                        .message("올바르지 않은 응답 상태입니다.")
                        .build();
            }

            teamInvitationRepository.save(invitation);

            String message = request.getStatus() == InvitationStatus.ACCEPTED ? 
                    "초대를 수락했습니다." : "초대를 거절했습니다.";

            return TeamInvitationDto.ApiResponse.builder()
                    .success(true)
                    .message(message)
                    .data(convertToResponse(invitation))
                    .build();

        } catch (Exception e) {
            log.error("초대 응답 실패: {}", e.getMessage(), e);
            return TeamInvitationDto.ApiResponse.builder()
                    .success(false)
                    .message("초대 응답에 실패했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 받은 초대 목록 조회
     */
    public List<TeamInvitationDto.InvitationResponse> getReceivedInvitations(Long userId) {
        List<TeamInvitation> invitations = teamInvitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(userId);
        return invitations.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 보낸 초대 목록 조회
     */
    public List<TeamInvitationDto.InvitationResponse> getSentInvitations(Long userId) {
        List<TeamInvitation> invitations = teamInvitationRepository.findByInviterUserIdOrderByCreatedAtDesc(userId);
        return invitations.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 엔티티를 DTO로 변환
     */
    private TeamInvitationDto.InvitationResponse convertToResponse(TeamInvitation invitation) {
        return TeamInvitationDto.InvitationResponse.builder()
                .invitationId(invitation.getId())
                .projectId(invitation.getProject().getProjectId())
                .projectTitle(invitation.getProject().getTitle())
                .projectDescription(invitation.getProject().getDescription())
                .inviterId(invitation.getInviter().getUserId())
                .inviterName(invitation.getInviter().getNickname())
                .inviteeId(invitation.getInvitee().getUserId())
                .inviteeName(invitation.getInvitee().getNickname())
                .status(invitation.getStatus())
                .message(invitation.getMessage())
                .createdAt(invitation.getCreatedAt())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }
}