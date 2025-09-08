package com.potential_radar.PR.user.service;

import com.potential_radar.PR.project.repository.TeamMemberReviewRepository;
import com.potential_radar.PR.user.domain.UserProfile;
import com.potential_radar.PR.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 💯 사용자 평판 점수 관리 서비스
 * 
 * 팀 프로젝트에서 받은 리뷰들을 기반으로 사용자의 평판 점수(reputation score)와
 * 리뷰 개수를 계산하여 UserProfile에 업데이트합니다.
 * 
 * 주요 기능:
 * - 특정 사용자의 리뷰 평균 점수 계산 및 업데이트
 * - 리뷰 개수 카운트 및 업데이트
 * - 트랜잭션 안전성 보장
 * 
 * 사용 시점:
 * - 새로운 팀 멤버 리뷰가 등록될 때
 * - 기존 리뷰가 수정/삭제될 때
 * - 배치 작업으로 전체 사용자 점수를 재계산할 때
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserReputationService {
    
    private final TeamMemberReviewRepository teamMemberReviewRepository;
    private final UserProfileRepository userProfileRepository;
    
    /**
     * 🎯 특정 사용자의 평판 점수 업데이트
     * 
     * 해당 사용자가 받은 모든 팀 멤버 리뷰를 집계하여
     * 평균 점수와 리뷰 개수를 계산한 후 UserProfile을 업데이트합니다.
     * 
     * @param userId 업데이트할 사용자 ID
     */
    public void updateUserReputation(Long userId) {
        log.info("📊 사용자 평판 점수 업데이트 시작: userId = {}", userId);
        
        // 1. 사용자 프로필 조회
        Optional<UserProfile> profileOpt = userProfileRepository.findById(userId);
        if (profileOpt.isEmpty()) {
            log.warn("⚠️ 사용자 프로필을 찾을 수 없음: userId = {}", userId);
            return;
        }
        
        UserProfile profile = profileOpt.get();
        
        // 2. 해당 사용자가 받은 리뷰들의 평균 점수와 개수 계산
        Object[] result = teamMemberReviewRepository.calculateUserReviewStats(userId);
        
        if (result != null && result.length == 2) {
            // 3. 평균 점수 계산 (소수점 둘째 자리까지)
            Double avgRating = (Double) result[0];
            Long reviewCount = (Long) result[1];
            
            BigDecimal reputationScore = avgRating != null 
                ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            
            int totalReviews = reviewCount != null ? reviewCount.intValue() : 0;
            
            // 4. UserProfile 업데이트
            profile.setReputationScore(reputationScore);
            profile.setReviewCount(totalReviews);
            
            userProfileRepository.save(profile);
            
            log.info("✅ 사용자 평판 점수 업데이트 완료: userId = {}, 평균점수 = {}, 리뷰개수 = {}", 
                    userId, reputationScore, totalReviews);
        } else {
            // 5. 리뷰가 없는 경우 기본값으로 설정
            profile.setReputationScore(BigDecimal.ZERO);
            profile.setReviewCount(0);
            
            userProfileRepository.save(profile);
            
            log.info("📝 리뷰가 없는 사용자 - 기본값으로 설정: userId = {}", userId);
        }
    }
    
    /**
     * 🔄 모든 사용자의 평판 점수 재계산 (배치용)
     * 
     * 전체 사용자의 평판 점수를 재계산합니다.
     * 주로 데이터 정합성 복구나 배치 작업에서 사용됩니다.
     */
    public void recalculateAllUserReputations() {
        log.info("🔄 모든 사용자의 평판 점수 재계산 시작");
        
        userProfileRepository.findAll().forEach(profile -> {
            updateUserReputation(profile.getUserId());
        });
        
        log.info("✅ 모든 사용자의 평판 점수 재계산 완료");
    }
}