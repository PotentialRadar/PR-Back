package com.potential_radar.PR.project.service;

import com.potential_radar.PR.common.exception.AccessDeniedException;
import com.potential_radar.PR.common.exception.DuplicateApplicationException;
import com.potential_radar.PR.common.exception.NotFoundException;
import com.potential_radar.PR.project.domain.*;
import com.potential_radar.PR.project.dto.ProjectApplicationResponseDTO;
import com.potential_radar.PR.project.dto.ProjectApplyRequest;
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse;
import com.potential_radar.PR.project.repository.*;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.notification.service.NotificationService;
import com.potential_radar.PR.notification.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectApplicationService {

    private final ProjectApplicationRepository ProjectApplicationRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectMemberRepository projectMemberRepository; //  멤버 저장소
    private final UserRepository userRepository;
    private final ProjectRecruitmentService projectRecruitmentService; // ProjectRecruitmentService 주입
    private final NotificationService notificationService;

    private ProjectApplicationResponseDTO convertToResponseDto(ProjectApplication application) {
        return ProjectApplicationResponseDTO.builder()
                .id(application.getId())
                .userId(application.getUser().getUserId())
                .userName(application.getUser().getNickname()) // 닉네임 추가
                .techPart(application.getTechPart())
                .applicationMessage(application.getApplicationMessage())
                .status(application.getStatus().name())
                .projectId(application.getProject().getProjectId())
                .projectTitle(application.getProject().getTitle())
                .projectStatus(application.getProject().getStatus().name())
                .build();
    }

    // 프로젝트 지원
    @Transactional
    public void applyProject(Long projectId, ProjectApplyRequest request, Long userId) {
        if (ProjectApplicationRepository.existsByProject_ProjectIdAndUser_UserId(projectId, userId)) {
            throw new DuplicateApplicationException("이미 지원하였습니다.");
        }

        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        ProjectApplication member = ProjectApplication.builder()
                .project(project)
                .user(user)
                .status(ProjectApplication.ApplicationStatus.PENDING)
                .techPart(request.getTechPart())
                .applicationMessage(request.getApplicationMessage())
                .build();

        ProjectApplicationRepository.save(member);

        // 프로젝트 팀 리더에게 지원 알림 전송
        if (project.getTeamLeader() != null) {
            sendApplicationNotificationAfterCommit(
                project.getTeamLeader(),
                user,
                project,
                request.getTechPart()
            );
        }
    }

    // 프로젝트 지원자 목록
    @Transactional(readOnly = true)
    public List<ProjectApplicationResponseDTO> getProjectMembers(Long projectId) {
        List<ProjectApplication> members = ProjectApplicationRepository.findByProject_ProjectId(projectId);
        return members.stream()
                .map(this::convertToResponseDto)
                .toList();
    }

    // 사용자가 지원한 프로젝트 목록
    @Transactional(readOnly = true)
    public List<ProjectRecruitmentResponse> getAppliedProjectsByUser(Long userId) {
        List<ProjectApplication> applications = ProjectApplicationRepository.findByUser_UserIdWithUser(userId);
        if (applications.isEmpty()) {
            return List.of();
        }
        String userEmail = applications.get(0).getUser().getEmail();

        return applications.stream()
                .map(application -> projectRecruitmentService.convertToResponseDto(application.getProject(), userEmail))
                .toList();
    }

    // 지원자 상태 업데이트 (승인 시 프로젝트 멤버 자동 편입)
    @Transactional
    public void updateMemberStatus(Long projectId, Long memberId, Long teamLeaderId, String status) {
        // 1) 프로젝트 조회
        ProjectRecruitment project = projectRecruitmentRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다."));

        // 2) 팀장 검증
        if (!Objects.equals(project.getTeamLeader().getUserId(), teamLeaderId)) {
            throw new AccessDeniedException("팀장만 승인/거절이 가능합니다.");
        }

        // 3) 지원자 조회
        ProjectApplication applicant = ProjectApplicationRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("지원자를 찾을 수 없습니다."));

        // 4) 해당 프로젝트 소속 지원자인지 검증
        if (!Objects.equals(applicant.getProject().getProjectId(), projectId)) {
            throw new AccessDeniedException("잘못된 접근입니다.");
        }

        // 5) 상태 변경
        if ("ACCEPTED".equalsIgnoreCase(status)) {
            applicant.setStatus(ProjectApplication.ApplicationStatus.ACCEPTED);

            // 승인 시 프로젝트 멤버 자동 편입 (중복 방지 + 동시성 안전)
            Long userId = applicant.getUser().getUserId();
            if (!projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(projectId, userId)) {
                try {
                    projectMemberRepository.save(ProjectMember.builder()
                            .project(project)
                            .user(applicant.getUser())
                            .role(ProjectMember.Role.MEMBER)
                            .techPart(applicant.getTechPart()) // 어떤 파트로 합류했는지 기록
                            .build());
                } catch (DataIntegrityViolationException ignore) {
                    // 동시에 두 번 승인 눌려도 unique 제약에 의해 한 번만 들어가도록 무시
                }
            }
            
            // 승인 알림 전송
            sendApprovalNotificationAfterCommit(applicant.getUser(), project);

        } else if ("REJECTED".equalsIgnoreCase(status)) {
            applicant.setStatus(ProjectApplication.ApplicationStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다.");
        }
    }

    // 트랜잭션 커밋 후 지원 알림 전송하는 메서드
    private void sendApplicationNotificationAfterCommit(User teamLeader, User applicant, ProjectRecruitment project, String techPart) {
        // Spring의 TransactionSynchronization을 사용하여 트랜잭션 커밋 후 실행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String notificationContent = String.format("%s님이 '%s' 프로젝트에 %s 포지션으로 지원했습니다.", 
                            applicant.getNickname(), project.getTitle(), techPart);
                    String url = String.format("/projects/%d", project.getProjectId());
                    
                    notificationService.send(
                        teamLeader, 
                        NotificationType.APPLICATION, 
                        notificationContent, 
                        url, 
                        null, 
                        LocalDateTime.now()
                    );
                } catch (Exception e) {
                    // 알림 전송 실패해도 지원 신청에는 영향 없도록 로그만 남김
                    System.err.println("지원 알림 전송 실패: " + e.getMessage());
                }
            }
        });
    }
    
    // 트랜잭션 커밋 후 승인 알림 전송하는 메서드
    private void sendApprovalNotificationAfterCommit(User applicant, ProjectRecruitment project) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String notificationContent = String.format("'%s' 프로젝트에 승인되었습니다!", project.getTitle());
                    String url = String.format("/projects/%d", project.getProjectId());
                    
                    notificationService.send(
                        applicant, 
                        NotificationType.APPLICATION_APPROVED, 
                        notificationContent, 
                        url, 
                        null, 
                        LocalDateTime.now()
                    );
                } catch (Exception e) {
                    System.err.println("승인 알림 전송 실패: " + e.getMessage());
                }
            }
        });
    }
}
