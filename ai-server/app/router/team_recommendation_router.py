from fastapi import APIRouter, HTTPException
from typing import List
import logging

from app.schemas import (
    RecommendMemberRequest, 
    RecommendedMember, 
    MemberExplanation,
    UserTechStack
)

router = APIRouter()
logger = logging.getLogger(__name__)

# 목업 데이터 (실제 AI 모델 연동 전까지 사용)
MOCK_USERS_DATABASE = [
    {
        "userId": 1,
        "name": "김개발자",
        "email": "kim.developer@example.com",
        "profileImage": "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
        "userTechStacks": [
            {"name": "React", "level": 4},
            {"name": "TypeScript", "level": 3},
            {"name": "JavaScript", "level": 5}
        ],
        "experience": "4년",
        "portfolioCount": 8,
        "completedProjects": 4,
        "averageRating": 4.6,
        "lastActiveDate": "2025-08-15",
        "isAvailable": True,
        "currentProjectCount": 1
    },
    {
        "userId": 3,
        "name": "이백엔드",
        "email": "lee.backend@example.com",
        "profileImage": "https://api.dicebear.com/7.x/avataaars/svg?seed=3",
        "userTechStacks": [
            {"name": "Node.js", "level": 5},
            {"name": "Python", "level": 4},
            {"name": "PostgreSQL", "level": 3}
        ],
        "experience": "5년",
        "portfolioCount": 15,
        "completedProjects": 2,
        "averageRating": 4.8,
        "lastActiveDate": "2025-08-14",
        "isAvailable": True,
        "currentProjectCount": 0
    },
    {
        "userId": 4,
        "name": "정모바일",
        "email": "jung.mobile@example.com",
        "profileImage": "https://api.dicebear.com/7.x/avataaars/svg?seed=4",
        "userTechStacks": [
            {"name": "Flutter", "level": 4},
            {"name": "React Native", "level": 3},
            {"name": "Firebase", "level": 4}
        ],
        "experience": "3년",
        "portfolioCount": 6,
        "completedProjects": 2,
        "averageRating": 4.2,
        "lastActiveDate": "2025-08-16",
        "isAvailable": True,
        "currentProjectCount": 0
    },
    {
        "userId": 5,
        "name": "최AI",
        "email": "choi.ai@example.com",
        "profileImage": "https://api.dicebear.com/7.x/avataaars/svg?seed=5",
        "userTechStacks": [
            {"name": "Python", "level": 5},
            {"name": "TensorFlow", "level": 4},
            {"name": "PyTorch", "level": 3}
        ],
        "experience": "4년",
        "portfolioCount": 12,
        "completedProjects": 2,
        "averageRating": 4.7,
        "lastActiveDate": "2025-08-17",
        "isAvailable": True,
        "currentProjectCount": 1
    },
    {
        "userId": 6,
        "name": "강데브옵스",
        "email": "kang.devops@example.com",
        "profileImage": "https://api.dicebear.com/7.x/avataaars/svg?seed=6",
        "userTechStacks": [
            {"name": "AWS", "level": 5},
            {"name": "Docker", "level": 4},
            {"name": "Kubernetes", "level": 4}
        ],
        "experience": "5년",
        "portfolioCount": 10,
        "completedProjects": 3,
        "averageRating": 4.5,
        "lastActiveDate": "2025-08-13",
        "isAvailable": True,
        "currentProjectCount": 0
    }
]

def calculate_match_score(user_skills: List[dict], required_skills: List[str]) -> float:
    """기술 스택 매칭 점수 계산"""
    if not required_skills:
        return 0.5
    
    user_skill_names = [skill["name"] for skill in user_skills]
    matched_skills = set(user_skill_names) & set(required_skills)
    
    if not matched_skills:
        return 0.2  # 기본 점수
    
    # 매칭된 기술의 수준을 고려한 점수 계산
    total_score = 0
    for skill in matched_skills:
        user_skill = next((s for s in user_skills if s["name"] == skill), None)
        if user_skill:
            total_score += user_skill["level"] / 5.0  # 5점 만점을 1점 만점으로 변환
    
    # 매칭된 기술 비율과 평균 수준을 결합
    match_ratio = len(matched_skills) / len(required_skills)
    avg_skill_level = total_score / len(matched_skills) if matched_skills else 0
    
    # 최종 점수 계산 (매칭 비율 70% + 기술 수준 30%)
    final_score = (match_ratio * 0.7) + (avg_skill_level * 0.3)
    return min(final_score, 1.0)

def generate_explanation(user: dict, required_skills: List[str], match_score: float) -> MemberExplanation:
    """추천 이유 생성"""
    user_skill_names = [skill["name"] for skill in user["userTechStacks"]]
    matched_skills = list(set(user_skill_names) & set(required_skills))
    growth_opportunities = list(set(required_skills) - set(user_skill_names))[:2]  # 최대 2개
    
    # 주요 이유 생성
    if matched_skills:
        main_reason = f"{', '.join(matched_skills[:2])} 경험이 프로젝트와 {int(match_score * 100)}% 일치합니다"
    else:
        main_reason = f"다양한 기술 경험으로 프로젝트에 기여할 수 있습니다"
    
    # 상세 이유 생성
    detailed_reasons = []
    for skill in matched_skills[:2]:
        skill_info = next((s for s in user["userTechStacks"] if s["name"] == skill), None)
        if skill_info:
            detailed_reasons.append(f"{skill} {skill_info['level']}년 경험으로 안정적인 개발 가능")
    
    # 간단한 설명 생성
    if matched_skills:
        simple_explanation = f"{matched_skills[0]} 전문가로 핵심 기능 개발 가능"
    else:
        simple_explanation = f"{user['experience']} 경력의 숙련된 개발자"
    
    # 경험 매칭 설명
    experience_years = int(user["experience"].replace("년", ""))
    if experience_years >= 5:
        experience_match = "고급 개발자로 팀 리드 경험 보유"
    elif experience_years >= 3:
        experience_match = "중급 개발자로 안정적인 개발 가능"
    else:
        experience_match = "주니어 개발자로 빠른 학습 능력 보유"
    
    return MemberExplanation(
        main_reason=main_reason,
        detailed_reasons=detailed_reasons,
        matched_skills=matched_skills,
        growth_opportunities=growth_opportunities,
        simple_explanation=simple_explanation,
        experience_match=experience_match
    )

@router.post("/recommend/members", response_model=List[RecommendedMember])
async def recommend_team_members(request: RecommendMemberRequest) -> List[RecommendedMember]:
    """팀원 추천 API"""
    logger.info(f"🤖 팀원 추천 요청 - 프로젝트 ID: {request.projectId}, 필요 기술: {request.requiredSkills}")
    
    try:
        # 1. 매칭 점수 계산 및 정렬
        candidates = []
        for user in MOCK_USERS_DATABASE:
            if not user["isAvailable"]:
                continue
                
            match_score = calculate_match_score(user["userTechStacks"], request.requiredSkills)
            explanation = generate_explanation(user, request.requiredSkills, match_score)
            
            recommended_member = RecommendedMember(
                userId=user["userId"],
                name=user["name"],
                email=user["email"],
                profileImage=user["profileImage"],
                matchScore=match_score,
                userTechStacks=[
                    UserTechStack(name=skill["name"], level=skill["level"]) 
                    for skill in user["userTechStacks"]
                ],
                explanation=explanation,
                experience=user["experience"],
                portfolioCount=user["portfolioCount"],
                completedProjects=user["completedProjects"],
                averageRating=user["averageRating"],
                lastActiveDate=user["lastActiveDate"],
                isAvailable=user["isAvailable"],
                currentProjectCount=user["currentProjectCount"]
            )
            
            candidates.append(recommended_member)
        
        # 2. 매칭 점수 순으로 정렬
        candidates.sort(key=lambda x: x.matchScore, reverse=True)
        
        # 3. 요청된 팀 크기만큼 반환
        result = candidates[:request.teamSize]
        
        logger.info(f"✅ 팀원 추천 완료 - 추천된 팀원 수: {len(result)}")
        return result
        
    except Exception as e:
        logger.error(f"❌ 팀원 추천 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=f"팀원 추천 중 오류가 발생했습니다: {str(e)}")