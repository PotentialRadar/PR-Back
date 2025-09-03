package com.potential_radar.PR.project.service;

import com.potential_radar.PR.notification.domain.NotificationType;
import com.potential_radar.PR.notification.service.NotificationService;
import com.potential_radar.PR.project.domain.ProjectMember;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectStatus;
import com.potential_radar.PR.project.repository.ProjectMemberRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectStatusUpdateService {

    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationService notificationService;

    // 매일 자정에 실행
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void updateProjectStatuses() {
        log.info("프로젝트 상태 업데이트 스케줄러 실행");
        updateRecruitingProjects();
        updateInProgressProjects();
    }

    private void updateRecruitingProjects() {
        LocalDate today = LocalDate.now();
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findByStatusAndRecruitDeadlineBefore(ProjectStatus.RECRUITING, today);

        for (ProjectRecruitment project : projects) {
            project.setStatus(ProjectStatus.IN_PROGRESS);
            log.info("프로젝트(ID: {}) 상태를 IN_PROGRESS로 변경 (사유: 모집 마감일 경과)", project.getProjectId());
        }
        projectRecruitmentRepository.saveAll(projects);
    }

    private void updateInProgressProjects() {
        LocalDate today = LocalDate.now();
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findByStatusAndEndDateBefore(ProjectStatus.IN_PROGRESS, today);

        for (ProjectRecruitment project : projects) {
            project.setStatus(ProjectStatus.COMPLETED);
            log.info("프로젝트(ID: {}) 상태를 COMPLETED로 변경 (사유: 종료일 경과)", project.getProjectId());
            sendReviewRequestNotificationAfterCommit(project);
        }
        projectRecruitmentRepository.saveAll(projects);
    }

    private void sendReviewRequestNotificationAfterCommit(ProjectRecruitment project) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    List<ProjectMember> members = projectMemberRepository.findAllByProject_ProjectId(project.getProjectId());
                    for (ProjectMember member : members) {
                        if (!member.getUser().getUserId().equals(project.getTeamLeader().getUserId())) {
                            String notificationContent = String.format("'%s' 프로젝트가 완료되었습니다. 리뷰를 작성해주세요!", project.getTitle());
                            String url = String.format("/projects/%d/review", project.getProjectId());
                            notificationService.send(
                                member.getUser(),
                                NotificationType.REVIEW_REMINDER,
                                notificationContent,
                                url,
                                null,
                                LocalDateTime.now()
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("리뷰 요청 알림 전송 실패: " + e.getMessage(), e);
                }
            }
        });
    }
}
