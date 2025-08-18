# app/utils/explanation_generator.py
"""
AI 추천 이유 설명 생성기
사용자에게 왜 특정 프로젝트를 추천했는지 명확하고 이해하기 쉬운 설명을 제공
"""

from typing import List, Dict, Any
# 간단한 기술스택 유사도 함수 (tech_similarity 대체)
def get_tech_similarity(tech1: str, tech2: str) -> float:
    """기본적인 기술스택 연관성 점수"""
    if tech1 == tech2:
        return 1.0
    
    # 간단한 연관성 매핑
    relations = {
        'JavaScript': ['React', 'Vue.js', 'Node.js', 'TypeScript'],
        'React': ['JavaScript', 'TypeScript', 'Node.js'],
        'Vue.js': ['JavaScript', 'TypeScript', 'Nuxt.js'],
        'Node.js': ['JavaScript', 'Express', 'MongoDB'],
        'Python': ['Django', 'FastAPI', 'PostgreSQL'],
        'Java': ['Spring Boot', 'PostgreSQL', 'Docker'],
        'TypeScript': ['JavaScript', 'React', 'Vue.js', 'Angular']
    }
    
    if tech1 in relations and tech2 in relations[tech1]:
        return 0.8
    if tech2 in relations and tech1 in relations[tech2]:
        return 0.8
    
    return 0.1
from .feature_engineering import jaccard, weighted_overlap
from .preprocess import to_name_list

class RecommendationExplainer:
    
    def __init__(self):
        self.explanation_templates = {
            'perfect_match': "🎯 완벽한 매치! {matched_techs}에서 전문성을 발휘할 수 있어요",
            'high_match': "✨ {matched_techs} 기술이 {match_percentage}% 일치해요",
            'related_techs': "🔗 {user_tech}를 알고 있다면 {project_tech}도 쉽게 배울 수 있어요",
            'skill_growth': "📈 {growth_techs}를 새로 배울 수 있는 좋은 기회예요",
            'popular_project': "🔥 {view_count}명이 관심을 가진 인기 프로젝트예요",
            'difficulty_match': "⚖️ 현재 기술 레벨에 적절한 난이도예요",
            'trending_tech': "📊 {trending_tech}는 요즘 많이 사용되는 트렌딩 기술이에요",
            'portfolio_boost': "💼 포트폴리오에 추가하면 좋을 프로젝트예요"
        }
    
    def generate_explanation(
        self, 
        user_techs: List[str],
        user_norm: List[Dict],
        project_techs: List[str], 
        project_norm: List[Dict],
        project_title: str,
        match_score: float,
        view_count: int = None
    ) -> Dict[str, Any]:
        """
        추천 이유를 분석하고 설명을 생성
        
        Returns:
            {
                'main_reason': '주요 추천 이유',
                'detailed_reasons': ['상세 이유1', '상세 이유2', ...],
                'score_breakdown': {
                    'tech_match': 0.8,
                    'similarity': 0.6,
                    'popularity': 0.3
                },
                'matched_skills': ['React', 'JavaScript'],
                'growth_opportunities': ['Node.js', 'Express']
            }
        """
        
        # 1. 기술스택 매치 분석
        user_set = set(user_techs)
        project_set = set(project_techs)
        matched_techs = user_set & project_set
        growth_techs = project_set - user_set
        
        # 2. 점수 분해
        jaccard_score = jaccard(user_techs, project_techs)
        weighted_score = weighted_overlap(user_norm, project_norm)
        similarity_scores = self._calculate_tech_similarities(user_techs, project_techs)
        
        # 3. 주요 추천 이유 결정
        main_reason = self._determine_main_reason(
            matched_techs, jaccard_score, similarity_scores, match_score
        )
        
        # 4. 상세 이유들 생성
        detailed_reasons = self._generate_detailed_reasons(
            user_techs, project_techs, matched_techs, growth_techs, 
            similarity_scores, view_count, jaccard_score
        )
        
        # 5. 성장 기회 분석
        growth_opportunities = self._analyze_growth_opportunities(
            user_techs, growth_techs
        )
        
        # 간단한 설명 생성
        simple_explanation = self.generate_simple_explanation(
            list(matched_techs), 
            growth_opportunities[:2]
        )
        
        return {
            'main_reason': main_reason,
            'detailed_reasons': detailed_reasons[:3],  # 최대 3개만
            'score_breakdown': {
                'tech_match': round(jaccard_score, 2),
                'skill_compatibility': round(weighted_score, 2),
                'tech_similarity': round(max(similarity_scores.values()) if similarity_scores else 0, 2)
            },
            'matched_skills': list(matched_techs),
            'growth_opportunities': growth_opportunities[:2],  # 최대 2개만
            'simple_explanation': simple_explanation
        }
    
    def _determine_main_reason(self, matched_techs, jaccard_score, similarity_scores, match_score):
        """주요 추천 이유를 결정"""
        
        if len(matched_techs) >= 3:
            return self.explanation_templates['perfect_match'].format(
                matched_techs=', '.join(list(matched_techs)[:3])
            )
        elif len(matched_techs) >= 1:
            match_percentage = int(jaccard_score * 100)
            return self.explanation_templates['high_match'].format(
                matched_techs=', '.join(matched_techs),
                match_percentage=match_percentage
            )
        elif similarity_scores:
            # 직접 매치는 없지만 연관성이 높은 경우
            best_pair = max(similarity_scores.items(), key=lambda x: x[1])
            user_tech, project_tech = best_pair[0].split(' → ')
            return self.explanation_templates['related_techs'].format(
                user_tech=user_tech,
                project_tech=project_tech
            )
        else:
            return self.explanation_templates['skill_growth'].format(
                growth_techs="새로운 기술"
            )
    
    def _generate_detailed_reasons(self, user_techs, project_techs, matched_techs, 
                                 growth_techs, similarity_scores, view_count, jaccard_score):
        """상세 추천 이유들을 생성"""
        reasons = []
        
        # 기술 연관성 설명
        if similarity_scores:
            top_similarities = sorted(similarity_scores.items(), key=lambda x: x[1], reverse=True)[:2]
            for tech_pair, score in top_similarities:
                if score > 0.7:  # 높은 연관성만
                    user_tech, project_tech = tech_pair.split(' → ')
                    reasons.append(
                        self.explanation_templates['related_techs'].format(
                            user_tech=user_tech, project_tech=project_tech
                        )
                    )
        
        # 성장 기회 설명
        if growth_techs:
            growth_list = list(growth_techs)[:2]
            reasons.append(
                self.explanation_templates['skill_growth'].format(
                    growth_techs=', '.join(growth_list)
                )
            )
        
        # 인기도 설명
        if view_count and view_count > 50:
            reasons.append(
                self.explanation_templates['popular_project'].format(
                    view_count=view_count
                )
            )
        
        # 난이도 적합성
        if 0.3 <= jaccard_score <= 0.8:  # 적절한 도전 수준
            reasons.append(self.explanation_templates['difficulty_match'])
        
        # 포트폴리오 가치
        if len(project_techs) >= 3:  # 다양한 기술을 사용하는 프로젝트
            reasons.append(self.explanation_templates['portfolio_boost'])
        
        return reasons
    
    def _calculate_tech_similarities(self, user_techs, project_techs):
        """사용자와 프로젝트 기술 간 연관성 점수 계산"""
        similarities = {}
        
        for user_tech in user_techs:
            for project_tech in project_techs:
                if user_tech != project_tech:  # 직접 매치 제외
                    similarity = get_tech_similarity(user_tech, project_tech)
                    if similarity > 0.5:  # 의미있는 연관성만
                        key = f"{user_tech} → {project_tech}"
                        similarities[key] = similarity
        
        return similarities
    
    def _analyze_growth_opportunities(self, user_techs, growth_techs):
        """성장 기회가 있는 기술들을 우선순위별로 정렬"""
        growth_opportunities = []
        
        for growth_tech in growth_techs:
            # 사용자 기술과의 연관성 확인
            max_similarity = 0
            for user_tech in user_techs:
                similarity = get_tech_similarity(user_tech, growth_tech)
                max_similarity = max(max_similarity, similarity)
            
            growth_opportunities.append((growth_tech, max_similarity))
        
        # 연관성이 높은 순으로 정렬
        growth_opportunities.sort(key=lambda x: x[1], reverse=True)
        
        return [tech for tech, _ in growth_opportunities]
    
    def generate_simple_explanation(self, matched_skills: List[str], growth_skills: List[str]) -> str:
        """간단한 한 줄 설명 생성 (프론트엔드용)"""
        
        if len(matched_skills) >= 2:
            return f"💡 {', '.join(matched_skills[:2])} 기술을 활용할 수 있어요"
        elif len(matched_skills) == 1:
            if growth_skills:
                return f"💡 {matched_skills[0]} 경험을 바탕으로 {growth_skills[0]}를 배울 수 있어요"
            else:
                return f"💡 {matched_skills[0]} 기술을 활용할 수 있어요"
        elif growth_skills:
            return f"🌱 {growth_skills[0]} 등 새로운 기술을 배울 수 있는 기회예요"
        else:
            return "🚀 도전적인 프로젝트로 역량을 키워보세요"


# 전역 explainer 인스턴스
explainer = RecommendationExplainer()