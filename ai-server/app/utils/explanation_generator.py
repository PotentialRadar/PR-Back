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
        # 더 자연스럽고 개인화된 설명 템플릿
        self.explanation_templates = {
            # 완벽한 매치
            'perfect_match': [
                "🎯 완벽해요! {matched_techs} 전문성을 마음껏 발휘하실 수 있어요",
                "✨ {matched_techs}가 주력인 당신에게 딱 맞는 프로젝트예요",
                "🔥 {matched_techs} 실력을 뽐낼 수 있는 멋진 프로젝트네요"
            ],
            # 높은 매치
            'high_match': [
                "👍 {matched_techs} 경험이 이 프로젝트와 잘 어울려요",
                "🌟 {matched_techs}를 잘하시니까 이 프로젝트도 재밌게 하실 것 같아요",
                "💫 {matched_techs} 기술로 멋진 결과물을 만들어보세요"
            ],
            # 관련 기술
            'related_techs': [
                "🔗 {user_tech} 경험이 있으시면 {project_tech}는 금방 익히실 거예요",
                "📚 {user_tech}에서 {project_tech}로 스킬을 확장해보는 건 어떨까요?",
                "🚀 {user_tech} 기반으로 {project_tech}를 배우면 시너지가 날 것 같아요"
            ],
            # 스킬 성장
            'skill_growth': [
                "🌱 {growth_techs}를 배워서 더 다재다능한 개발자가 되어보세요",
                "📈 {growth_techs} 기술을 추가하면 커리어에 도움이 될 거예요",
                "💪 {growth_techs}로 새로운 도전을 시작해보시는 건 어떨까요?"
            ],
            # 인기 프로젝트
            'popular_project': [
                "🔥 많은 분들이 관심을 보이고 있는 핫한 프로젝트예요",
                "👥 인기가 많은 프로젝트라서 좋은 팀원들을 만날 수 있을 거예요",
                "⭐ 화제가 되고 있는 프로젝트에 참여해보세요"
            ],
            # 적절한 난이도
            'difficulty_match': [
                "⚖️ 지금 실력에 딱 맞는 도전적인 레벨이에요",
                "🎯 적당히 도전적이면서도 해볼 만한 프로젝트네요",
                "💪 현재 수준에서 한 단계 더 성장할 수 있을 것 같아요"
            ],
            # 포트폴리오 가치
            'portfolio_boost': [
                "💼 포트폴리오에 추가하면 정말 임팩트 있을 것 같아요",
                "🏆 이런 프로젝트 경험은 이력서에 금상첨화가 될 거예요",
                "✨ 다양한 기술을 사용해서 포트폴리오 퀄리티가 확 올라갈 거예요"
            ],
            # 피드백 기반 개인화 (새로 추가)
            'feedback_personalized': [
                "😊 최근 좋아요 누르신 프로젝트와 비슷한 스타일이에요",
                "🎯 이전에 관심 보이신 {preferred_tech} 기반 프로젝트입니다",
                "💝 당신 취향에 딱 맞는 프로젝트를 발견했어요",
                "🔍 피드백을 보니 이런 프로젝트를 선호하시는 것 같더라고요"
            ]
        }
    
    def generate_explanation(
        self, 
        user_techs: List[str],
        user_norm: List[Dict],
        project_techs: List[str], 
        project_norm: List[Dict],
        project_title: str,
        match_score: float,
        view_count: int = None,
        user_feedback_stats: Dict = None  # 🆕 피드백 기반 개인화
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
        
        # 3. 주요 추천 이유 결정 (피드백 기반 개인화 포함)
        main_reason = self._determine_main_reason(
            matched_techs, jaccard_score, similarity_scores, match_score, user_feedback_stats
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
        
        # 간단한 설명 생성 (피드백 기반 개인화 포함)
        simple_explanation = self.generate_simple_explanation(
            list(matched_techs), 
            growth_opportunities[:2],
            user_feedback_stats
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
    
    def _determine_main_reason(self, matched_techs, jaccard_score, similarity_scores, match_score, user_feedback_stats=None):
        """주요 추천 이유를 결정 (개인화 + 랜덤성)"""
        import random
        
        # 🆕 피드백 기반 개인화 우선 체크
        if user_feedback_stats and user_feedback_stats.get('totalFeedbacks', 0) >= 2:
            preferred_techs = user_feedback_stats.get('preferredTechStacks', [])
            matched_preferred = set(matched_techs) & set(preferred_techs)
            
            if matched_preferred:
                # 선호하는 기술이 매칭된 경우 개인화 메시지
                template = random.choice(self.explanation_templates['feedback_personalized'])
                return template.format(preferred_tech=list(matched_preferred)[0])
        
        # 기존 로직 + 랜덤 템플릿 선택
        if len(matched_techs) >= 3:
            template = random.choice(self.explanation_templates['perfect_match'])
            return template.format(matched_techs=', '.join(list(matched_techs)[:3]))
            
        elif len(matched_techs) >= 1:
            template = random.choice(self.explanation_templates['high_match'])
            return template.format(matched_techs=', '.join(matched_techs))
            
        elif similarity_scores:
            # 직접 매치는 없지만 연관성이 높은 경우
            best_pair = max(similarity_scores.items(), key=lambda x: x[1])
            user_tech, project_tech = best_pair[0].split(' → ')
            template = random.choice(self.explanation_templates['related_techs'])
            return template.format(user_tech=user_tech, project_tech=project_tech)
            
        else:
            template = random.choice(self.explanation_templates['skill_growth'])
            return template.format(growth_techs="새로운 기술")
    
    def _generate_detailed_reasons(self, user_techs, project_techs, matched_techs, 
                                 growth_techs, similarity_scores, view_count, jaccard_score):
        """상세 추천 이유들을 생성 (랜덤성 추가)"""
        import random
        reasons = []
        
        # 기술 연관성 설명
        if similarity_scores:
            top_similarities = sorted(similarity_scores.items(), key=lambda x: x[1], reverse=True)[:2]
            for tech_pair, score in top_similarities:
                if score > 0.7:  # 높은 연관성만
                    user_tech, project_tech = tech_pair.split(' → ')
                    template = random.choice(self.explanation_templates['related_techs'])
                    reasons.append(template.format(user_tech=user_tech, project_tech=project_tech))
        
        # 성장 기회 설명
        if growth_techs:
            growth_list = list(growth_techs)[:2]
            template = random.choice(self.explanation_templates['skill_growth'])
            reasons.append(template.format(growth_techs=', '.join(growth_list)))
        
        # 인기도 설명
        if view_count and view_count > 50:
            template = random.choice(self.explanation_templates['popular_project'])
            reasons.append(template)
        
        # 난이도 적합성
        if 0.3 <= jaccard_score <= 0.8:  # 적절한 도전 수준
            template = random.choice(self.explanation_templates['difficulty_match'])
            reasons.append(template)
        
        # 포트폴리오 가치
        if len(project_techs) >= 3:  # 다양한 기술을 사용하는 프로젝트
            template = random.choice(self.explanation_templates['portfolio_boost'])
            reasons.append(template)
        
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
    
    def generate_simple_explanation(self, matched_skills: List[str], growth_skills: List[str], 
                                  user_feedback_stats: Dict = None) -> str:
        """간단한 한 줄 설명 생성 (프론트엔드용 - 개인화 + 랜덤성)"""
        import random
        
        # 🆕 피드백 기반 개인화 메시지 (우선)
        if user_feedback_stats and user_feedback_stats.get('totalFeedbacks', 0) >= 3:
            preferred_techs = user_feedback_stats.get('preferredTechStacks', [])
            matched_preferred = set(matched_skills) & set(preferred_techs)
            
            if matched_preferred:
                messages = [
                    f"😊 좋아하시는 {list(matched_preferred)[0]} 기반 프로젝트예요",
                    f"🎯 {list(matched_preferred)[0]} 취향저격 프로젝트 발견!",
                    f"💫 {list(matched_preferred)[0]}로 재밌게 개발하실 것 같아요"
                ]
                return random.choice(messages)
        
        # 기존 로직 + 다양한 표현
        if len(matched_skills) >= 2:
            messages = [
                f"💡 {', '.join(matched_skills[:2])} 전문성을 발휘해보세요",
                f"🔥 {', '.join(matched_skills[:2])} 실력을 뽐낼 차례예요",
                f"✨ {', '.join(matched_skills[:2])}로 멋진 작품을 만들어보세요"
            ]
            return random.choice(messages)
            
        elif len(matched_skills) == 1:
            if growth_skills:
                messages = [
                    f"🚀 {matched_skills[0]}에서 {growth_skills[0]}로 스킬업!",
                    f"📚 {matched_skills[0]} 기반으로 {growth_skills[0]}를 익혀보세요",
                    f"🌱 {matched_skills[0]} 경험이 {growth_skills[0]} 학습에 도움될 거예요"
                ]
                return random.choice(messages)
            else:
                messages = [
                    f"💪 {matched_skills[0]} 실력을 더욱 키워보세요",
                    f"🎯 {matched_skills[0]} 전문가의 길로!",
                    f"⭐ {matched_skills[0]} 마스터가 되어보세요"
                ]
                return random.choice(messages)
                
        elif growth_skills:
            messages = [
                f"🌟 {growth_skills[0]} 등 새로운 기술에 도전해보세요",
                f"🚀 {growth_skills[0]}로 새로운 영역을 개척해보세요",
                f"📈 {growth_skills[0]} 습득으로 한 단계 업그레이드!"
            ]
            return random.choice(messages)
            
        else:
            messages = [
                "🔥 새로운 도전으로 성장의 기회를 잡으세요",
                "💪 도전적인 프로젝트로 실력을 늘려보세요",
                "🌟 새로운 영역에서 가능성을 발견해보세요"
            ]
            return random.choice(messages)


# 전역 explainer 인스턴스
explainer = RecommendationExplainer()