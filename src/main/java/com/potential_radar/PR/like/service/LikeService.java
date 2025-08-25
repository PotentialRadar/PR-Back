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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String LIKE_COUNT_KEY_PREFIX = "likeCount::";

    //좋아요 클릭
    @Transactional
    public LikeResponseDto toggleLike(LikeRequestDto requestDto, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        likeRepository.findByUserAndTargetTypeAndTargetId(
                user, requestDto.getTargetType(), requestDto.getTargetId()
        ).ifPresentOrElse(
                likeRepository::delete,
                () -> {
                    Like like = new Like(user, requestDto.getTargetType(), requestDto.getTargetId());
                    likeRepository.save(like);
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
     private String generateLikeCountKey(TargetType targetType, Long targetId) {
        return LIKE_COUNT_KEY_PREFIX + targetType.name() + "::" + targetId;
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(TargetType targetType, Long targetId, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));
        return likeRepository.findByUserAndTargetTypeAndTargetId(user, targetType, targetId).isPresent();
    }
}
