package com.potential_radar.PR.like.service;

import com.potential_radar.PR.common.exception.NotFoundException;
import com.potential_radar.PR.like.domain.Like;
import com.potential_radar.PR.like.domain.TargetType;
import com.potential_radar.PR.like.dto.LikeRequestDto;
import com.potential_radar.PR.like.dto.LikeResponseDto;
import com.potential_radar.PR.like.repository.LikeRepository;
import com.potential_radar.PR.project.domain.ProjectApplication;
import com.potential_radar.PR.project.domain.ProjectRecruitment;
import com.potential_radar.PR.project.dto.ProjectPartRecruitmentDTO;
import com.potential_radar.PR.project.dto.ProjectRecruitmentResponse;
import com.potential_radar.PR.project.dto.ProjectTechStackDTO;
import com.potential_radar.PR.project.repository.ProjectApplicationRepository;
import com.potential_radar.PR.project.repository.ProjectRecruitmentRepository;
import com.potential_radar.PR.project.service.ProjectRecruitmentService;
import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import com.potential_radar.PR.notification.service.NotificationService;
import com.potential_radar.PR.notification.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectRecruitmentService projectRecruitmentService; // ProjectRecruitmentService 주입
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;
    private final com.potential_radar.PR.user.service.PortfolioService portfolioService;

    private static final String LIKE_COUNT_KEY_PREFIX = "likeCount::";

    //좋아요 클릭
    @Transactional
    public LikeResponseDto toggleLike(LikeRequestDto requestDto, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        boolean wasLiked = likeRepository.findByUserAndTargetTypeAndTargetId(
                user, requestDto.getTargetType(), requestDto.getTargetId()
        ).isPresent();
        
        likeRepository.findByUserAndTargetTypeAndTargetId(
                user, requestDto.getTargetType(), requestDto.getTargetId()
        ).ifPresentOrElse(
                likeRepository::delete,
                () -> {
                    Like like = new Like(user, requestDto.getTargetType(), requestDto.getTargetId());
                    likeRepository.save(like);
                    // 좋아요 추가 시에만 알림 전송 (좋아요 취소는 알림 안함)
                    sendLikeNotificationAfterCommit(user, requestDto);
                }
        );

        long likeCount = likeRepository.countByTargetTypeAndTargetId(requestDto.getTargetType(), requestDto.getTargetId());
        boolean isLiked = likeRepository.findByUserAndTargetTypeAndTargetId(user, requestDto.getTargetType(), requestDto.getTargetId()).isPresent();

        //캐시 업데이트
        String key = generateLikeCountKey(requestDto.getTargetType(), requestDto.getTargetId());
        redisTemplate.opsForValue().set(key, likeCount);

        return new LikeResponseDto(likeCount, isLiked);
    }

    //좋아요 개수
    @Transactional(readOnly = true)
    public long getLikeCount(TargetType targetType, Long targetId) {
        String key = generateLikeCountKey(targetType, targetId);
        Object cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue != null) {
            return ((Number) cachedValue).longValue();
        } else {
            long likeCount = likeRepository.countByTargetTypeAndTargetId(targetType, targetId);
            redisTemplate.opsForValue().set(key, likeCount, 1, TimeUnit.HOURS); // 1시간 동안 캐시
            return likeCount;
        }
    }


    //프로젝트 좋아요 조회
    @Transactional(readOnly = true)
    public List<ProjectRecruitmentResponse> getLikedProjects(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        List<Like> likes = likeRepository.findByUserAndTargetType(user, TargetType.PROJECT);
        if (likes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> projectIds = likes.stream()
                .map(Like::getTargetId)
                .collect(Collectors.toList());

        List<ProjectRecruitment> projects = projectRecruitmentRepository.findAllById(projectIds);
        logger.debug("Number of projects found for liked projects: {}", projects.size());
        List<ProjectRecruitmentResponse> responses = new ArrayList<>();

        for (ProjectRecruitment pr : projects) {
            responses.add(projectRecruitmentService.convertToResponseDto(pr, user.getEmail()));
        }
        return responses;
    }

    //포트폴리오 좋아요 조회
    @Transactional(readOnly = true)
    public List<com.potential_radar.PR.user.dto.portfolios.PortfolioSummaryResponse> getLikedPortfolios(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        List<Like> likes = likeRepository.findByUserAndTargetType(user, TargetType.PORTFOLIO);
        if (likes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> portfolioOwnerIds = likes.stream()
                .map(Like::getTargetId)
                .collect(Collectors.toList());

        return portfolioOwnerIds.stream()
                .map(portfolioService::getPortfolioSummary)
                .collect(Collectors.toList());
    }

     private String generateLikeCountKey(TargetType targetType, Long targetId) {
        return LIKE_COUNT_KEY_PREFIX + targetType.name() + "::" + targetId;
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(TargetType targetType, Long targetId, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));
        return likeRepository.findByUserAndTargetTypeAndTargetId(user, targetType, targetId).isPresent();
    }
    
    // 트랜잭션 커밋 후 좋아요 알림 전송하는 메서드
    private void sendLikeNotificationAfterCommit(User liker, LikeRequestDto requestDto) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    if (requestDto.getTargetType() == TargetType.PROJECT) {
                        // 프로젝트 좋아요 알림
                        ProjectRecruitment project = projectRecruitmentRepository.findById(requestDto.getTargetId())
                            .orElse(null);
                        if (project != null && !liker.getUserId().equals(project.getTeamLeader().getUserId())) {
                            String notificationContent = String.format("%s님이 '%s' 프로젝트에 좋아요를 눌렀습니다.", 
                                liker.getNickname(), project.getTitle());
                            String url = String.format("/projects/%d", project.getProjectId());
                            
                            notificationService.send(
                                project.getTeamLeader(), 
                                NotificationType.LIKE, 
                                notificationContent, 
                                url, 
                                null, 
                                LocalDateTime.now()
                            );
                        }
                    } else if (requestDto.getTargetType() == TargetType.PORTFOLIO) {
                        // 포트폴리오 좋아요 알림 (User 엔티티에서 포트폴리오 소유자 찾기)
                        User portfolioOwner = userRepository.findById(requestDto.getTargetId())
                            .orElse(null);
                        if (portfolioOwner != null && !liker.getUserId().equals(portfolioOwner.getUserId())) {
                            String notificationContent = String.format("%s님이 회원님의 포트폴리오에 좋아요를 눌렀습니다.", 
                                liker.getNickname());
                            String url = String.format("/portfolios/%d", portfolioOwner.getUserId());
                            
                            notificationService.send(
                                portfolioOwner, 
                                NotificationType.LIKE, 
                                notificationContent, 
                                url, 
                                null, 
                                LocalDateTime.now()
                            );
                        }
                    }
                } catch (Exception e) {
                    System.err.println("좋아요 알림 전송 실패: " + e.getMessage());
                }
            }
        });
    }
}
