"""
좋아요 데이터 분석 유틸리티
사용자의 좋아요 패턴을 분석하여 추천에 활용
"""
from typing import List, Dict, Set
from datetime import datetime, timedelta
from collections import Counter, defaultdict
import logging

from app.schemas import LikedProject

logger = logging.getLogger(__name__)

class LikePatternAnalyzer:
    """사용자 좋아요 패턴 분석기"""
    
    def __init__(self):
        self.recent_days_threshold = 30  # 최근 30일
        self.category_weight = 0.3
        self.tech_weight = 0.7
    
    def analyze_user_preferences(self, liked_projects: List[LikedProject]) -> Dict:
        """
        사용자의 좋아요 패턴을 종합 분석
        
        Args:
            liked_projects: 사용자가 좋아요한 프로젝트 리스트
            
        Returns:
            분석 결과 딕셔너리
        """
        if not liked_projects:
            return {
                "preferred_techs": {},
                "preferred_categories": {},
                "recent_preference_weight": 0.0,
                "total_likes": 0
            }
        
        # 1. 선호 기술스택 분석
        preferred_techs = self._analyze_tech_preferences(liked_projects)
        
        # 2. 선호 카테고리 분석  
        preferred_categories = self._analyze_category_preferences(liked_projects)
        
        # 3. 시간 가중치 계산
        recent_weight = self._calculate_recent_preference_weight(liked_projects)
        
        # 4. 기술스택 조합 패턴 분석
        tech_combinations = self._analyze_tech_combinations(liked_projects)
        
        result = {
            "preferred_techs": preferred_techs,
            "preferred_categories": preferred_categories,
            "tech_combinations": tech_combinations,
            "recent_preference_weight": recent_weight,
            "total_likes": len(liked_projects)
        }
        
        logger.info(f"좋아요 패턴 분석 완료 - 총 {len(liked_projects)}개 프로젝트, "
                   f"주요 기술: {list(preferred_techs.keys())[:3]}")
        
        return result
    
    def _analyze_tech_preferences(self, liked_projects: List[LikedProject]) -> Dict[str, float]:
        """기술스택 선호도 분석"""
        tech_counter = Counter()
        total_projects = len(liked_projects)
        
        # 시간 가중치 적용
        now = datetime.now()
        
        for project in liked_projects:
            # 최근일수록 높은 가중치
            days_ago = (now - project.likedAt).days
            time_weight = max(0.1, 1.0 - (days_ago / 365))  # 1년 전까지 고려
            
            for tech in project.techStacks:
                tech_normalized = tech.lower().strip()
                tech_counter[tech_normalized] += time_weight
        
        # 정규화 (0-1 범위)
        if tech_counter:
            max_count = max(tech_counter.values())
            return {tech: count/max_count for tech, count in tech_counter.most_common(10)}
        
        return {}
    
    def _analyze_category_preferences(self, liked_projects: List[LikedProject]) -> Dict[str, float]:
        """카테고리 선호도 분석"""
        category_counter = Counter()
        
        for project in liked_projects:
            category_counter[project.category] += 1
        
        total = sum(category_counter.values())
        if total > 0:
            return {cat: count/total for cat, count in category_counter.most_common(5)}
        
        return {}
    
    def _calculate_recent_preference_weight(self, liked_projects: List[LikedProject]) -> float:
        """최근 활동 가중치 계산"""
        if not liked_projects:
            return 0.0
        
        now = datetime.now()
        recent_threshold = now - timedelta(days=self.recent_days_threshold)
        
        recent_likes = [p for p in liked_projects if p.likedAt >= recent_threshold]
        
        return min(1.0, len(recent_likes) / len(liked_projects) * 2)  # 최대 1.0
    
    def _analyze_tech_combinations(self, liked_projects: List[LikedProject]) -> Dict[str, int]:
        """기술스택 조합 패턴 분석"""
        combinations = Counter()
        
        for project in liked_projects:
            if len(project.techStacks) >= 2:
                # 2개 이상 기술스택 조합 분석
                sorted_techs = sorted([tech.lower().strip() for tech in project.techStacks])
                for i in range(len(sorted_techs)):
                    for j in range(i+1, len(sorted_techs)):
                        combo = f"{sorted_techs[i]}+{sorted_techs[j]}"
                        combinations[combo] += 1
        
        return dict(combinations.most_common(5))
    
    def calculate_like_similarity_score(self, user_preferences: Dict, project_techs: List[str], 
                                      project_category: str = "기타") -> float:
        """
        사용자 선호도와 프로젝트 간의 유사도 점수 계산
        
        Args:
            user_preferences: analyze_user_preferences 결과
            project_techs: 프로젝트 기술스택
            project_category: 프로젝트 카테고리
            
        Returns:
            유사도 점수 (0.0-1.0)
        """
        if not user_preferences.get("preferred_techs"):
            return 0.0
        
        # 1. 기술스택 매칭 점수
        tech_score = 0.0
        preferred_techs = user_preferences["preferred_techs"]
        
        project_techs_normalized = [tech.lower().strip() for tech in project_techs]
        
        for tech in project_techs_normalized:
            if tech in preferred_techs:
                tech_score += preferred_techs[tech]
        
        # 프로젝트 기술스택 수로 정규화
        if project_techs_normalized:
            tech_score = min(1.0, tech_score / len(project_techs_normalized))
        
        # 2. 카테고리 매칭 점수
        category_score = 0.0
        preferred_categories = user_preferences.get("preferred_categories", {})
        if project_category in preferred_categories:
            category_score = preferred_categories[project_category]
        
        # 3. 최종 점수 계산 (기술스택 70%, 카테고리 30%)
        final_score = (tech_score * self.tech_weight) + (category_score * self.category_weight)
        
        # 4. 최근 활동 가중치 적용
        recent_weight = user_preferences.get("recent_preference_weight", 0.0)
        final_score = final_score * (0.8 + 0.2 * recent_weight)  # 최소 80%, 최대 100%
        
        return min(1.0, final_score)