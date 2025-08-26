package com.potential_radar.PR.notification.service;

import com.potential_radar.PR.notification.domain.NotificationType;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.domain.ProjectStatus;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineNotificationService {

    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final NotificationService notificationService;

    // 매일 오전 9시에 실행 - 지원마감일 3일 전 알림
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(readOnly = true)
    public void sendDeadlineImminentNotifications() {
        LocalDate threeDaysLater = LocalDate.now().plusDays(3);
        
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findByRecruitDeadlineAndStatus(
            threeDaysLater, ProjectStatus.RECRUITING);
        
        for (ProjectRecruitment project : projects) {
            try {
                String notificationContent = String.format("'%s' 프로젝트의 지원마감일이 3일 후입니다.", 
                    project.getTitle());
                String url = String.format("/projects/%d", project.getProjectId());
                
                notificationService.send(
                    project.getTeamLeader(),
                    NotificationType.DEADLINE_IMMINENT,
                    notificationContent,
                    url,
                    null,
                    LocalDateTime.now()
                );
                
                log.info("3일 후 마감 알림 전송 완료: 프로젝트 ID {}, PM {}", 
                    project.getProjectId(), project.getTeamLeader().getNickname());
                    
            } catch (Exception e) {
                log.error("3일 후 마감 알림 전송 실패: 프로젝트 ID {}, 오류: {}", 
                    project.getProjectId(), e.getMessage());
            }
        }
    }

    // 매일 오전 9시에 실행 - 지원마감일 당일 알림
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(readOnly = true)
    public void sendDeadlineTodayNotifications() {
        LocalDate today = LocalDate.now();
        
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findByRecruitDeadlineAndStatus(
            today, ProjectStatus.RECRUITING);
        
        for (ProjectRecruitment project : projects) {
            try {
                String notificationContent = String.format("'%s' 프로젝트의 지원마감일이 오늘입니다!", 
                    project.getTitle());
                String url = String.format("/projects/%d", project.getProjectId());
                
                notificationService.send(
                    project.getTeamLeader(),
                    NotificationType.DEADLINE_TODAY,
                    notificationContent,
                    url,
                    null,
                    LocalDateTime.now()
                );
                
                log.info("당일 마감 알림 전송 완료: 프로젝트 ID {}, PM {}", 
                    project.getProjectId(), project.getTeamLeader().getNickname());
                    
            } catch (Exception e) {
                log.error("당일 마감 알림 전송 실패: 프로젝트 ID {}, 오류: {}", 
                    project.getProjectId(), e.getMessage());
            }
        }
    }
}