"""
피드백 기반 추천 개선 서비스
Spring Boot에서 사용자 피드백을 조회하여 추천 알고리즘에 반영
"""

import requests
import logging
from typing import Dict, List, Optional
import json

logger = logging.getLogger(__name__)

class FeedbackService:
    def __init__(self, spring_boot_url: str = "http://localhost:8080"):
        self.spring_boot_url = spring_boot_url
    
    def get_user_feedback_stats(self, user_id: int) -> Dict:
        """
        Spring Boot API에서 사용자 피드백 통계 조회
        """
        try:
            url = f"{self.spring_boot_url}/api/recommend/users/{user_id}/feedback-stats"
            response = requests.get(url, timeout=10)
            
            if response.status_code == 200:
                stats = response.json()
                logger.info(f"✅ 사용자 {user_id} 피드백 통계 조회 성공: {stats}")
                return stats
            else:
                logger.warning(f"⚠️ 피드백 통계 조회 실패 (HTTP {response.status_code})")
                return self._get_default_stats(user_id)
                
        except requests.RequestException as e:
            logger.warning(f"⚠️ Spring Boot API 연결 실패: {e}")
            return self._get_default_stats(user_id)
        except Exception as e:
            logger.error(f"❌ 피드백 통계 조회 중 오류: {e}")
            return self._get_default_stats(user_id)
    
    def _get_default_stats(self, user_id: int) -> Dict:
        """기본 피드백 통계 (API 호출 실패 시)"""
        return {
            "userId": user_id,
            "totalFeedbacks": 0,
            "likeCount": 0,
            "dislikeCount": 0,
            "likeRatio": 0.5,
            "hasEnoughData": False,
            "preferredTechStacks": []
        }
    
    def adjust_recommendation_score(self, base_score: float, user_id: int, 
                                   project_tech_stacks: List[str]) -> float:
        """
        피드백 기반으로 추천 점수 조정
        
        Args:
            base_score: 기존 추천 점수
            user_id: 사용자 ID  
            project_tech_stacks: 프로젝트의 기술스택 목록
        
        Returns:
            조정된 추천 점수
        """
        try:
            feedback_stats = self.get_user_feedback_stats(user_id)
            
            # 충분한 피드백 데이터가 없으면 기존 점수 유지
            if not feedback_stats.get("hasEnoughData", False):
                logger.debug(f"사용자 {user_id}: 피드백 데이터 부족, 기존 점수 유지")
                return base_score
            
            # 좋아요 비율 기반 조정
            like_ratio = feedback_stats.get("likeRatio", 0.5)
            total_feedbacks = feedback_stats.get("totalFeedbacks", 0)
            
            # 기본 피드백 보정
            feedback_multiplier = 1.0
            
            if like_ratio >= 0.8:  # 매우 만족 (80% 이상 좋아요)
                feedback_multiplier = 1.15  # 15% 보너스
            elif like_ratio >= 0.6:  # 만족 (60% 이상 좋아요)  
                feedback_multiplier = 1.1   # 10% 보너스
            elif like_ratio <= 0.2:  # 매우 불만족 (20% 이하 좋아요)
                feedback_multiplier = 0.85  # 15% 페널티
            elif like_ratio <= 0.4:  # 불만족 (40% 이하 좋아요)
                feedback_multiplier = 0.9   # 10% 페널티
            
            # 선호 기술스택 매칭 보너스
            preferred_techs = feedback_stats.get("preferredTechStacks", [])
            if preferred_techs and project_tech_stacks:
                # 프로젝트 기술스택과 선호 기술스택의 교집합 계산
                project_techs_lower = [tech.lower() for tech in project_tech_stacks]
                matching_techs = set(preferred_techs) & set(project_techs_lower)
                
                if matching_techs:
                    # 매칭된 기술스택 비율에 따른 보너스 (최대 20%)
                    match_ratio = len(matching_techs) / len(preferred_techs)
                    preferred_bonus = 1.0 + (match_ratio * 0.2)  # 최대 20% 보너스
                    feedback_multiplier *= preferred_bonus
                    
                    logger.info(f"🎯 사용자 {user_id} 선호 기술 매칭: {matching_techs} "
                              f"(보너스: +{match_ratio*20:.1f}%)")
            
            adjusted_score = base_score * feedback_multiplier
            
            logger.info(f"📊 사용자 {user_id} 피드백 보정: {base_score:.3f} → {adjusted_score:.3f} "
                       f"(좋아요율: {like_ratio:.1%}, 총피드백: {total_feedbacks}개)")
            
            return adjusted_score
            
        except Exception as e:
            logger.error(f"❌ 추천 점수 조정 실패: {e}")
            return base_score
    
    def log_feedback_impact(self, user_id: int):
        """피드백 영향 분석 로그"""
        try:
            stats = self.get_user_feedback_stats(user_id)
            
            if stats.get("totalFeedbacks", 0) > 0:
                logger.info(f"🔍 사용자 {user_id} 피드백 분석:")
                logger.info(f"  - 총 피드백: {stats['totalFeedbacks']}개")
                logger.info(f"  - 좋아요 비율: {stats['likeRatio']:.1%}")
                logger.info(f"  - 선호 기술: {stats.get('preferredTechStacks', [])[:5]}")
            else:
                logger.info(f"📝 사용자 {user_id}: 피드백 데이터 없음")
                
        except Exception as e:
            logger.warning(f"⚠️ 피드백 영향 분석 실패: {e}")

# 전역 인스턴스
feedback_service = FeedbackService()