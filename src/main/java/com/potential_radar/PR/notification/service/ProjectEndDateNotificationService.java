package com.potential_radar.PR.notification.service;

import com.potential_radar.PR.notification.domain.NotificationType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectEndDateNotificationService {

    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationService notificationService;

    // 매일 오전 10시에 실행 - 종료일+3일 경과한 프로젝트의 멤버들에게 리뷰 요청 알림
    @Scheduled(cron = "0 0 10 * * ?")
    @Transactional(readOnly = true)
    public void sendReviewRequestForOverdueProjects() {
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        
        // 종료일이 3일 전이고, 아직 COMPLETED 상태가 아닌 IN_PROGRESS 프로젝트들 조회
        List<ProjectRecruitment> projects = projectRecruitmentRepository.findByEndDateAndStatus(
            threeDaysAgo, ProjectStatus.IN_PROGRESS);
        
        for (ProjectRecruitment project : projects) {
            try {
                // 프로젝트 멤버들 조회
                List<ProjectMember> members = projectMemberRepository.findAllByProject_ProjectId(project.getProjectId());
                
                for (ProjectMember member : members) {
                    // 팀 리더는 제외하고 멤버들에게만 리뷰 요청 알림 전송
                    if (!member.getUser().getUserId().equals(project.getTeamLeader().getUserId())) {
                        String notificationContent = String.format("'%s' 프로젝트 종료일이 지났습니다. 리뷰를 작성해주세요!", 
                            project.getTitle());
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
                
                log.info("종료일+3일 리뷰 요청 알림 전송 완료: 프로젝트 ID {}, 제목 '{}'", 
                    project.getProjectId(), project.getTitle());
                    
            } catch (Exception e) {
                log.error("종료일+3일 리뷰 요청 알림 전송 실패: 프로젝트 ID {}, 오류: {}", 
                    project.getProjectId(), e.getMessage());
            }
        }
    }
}