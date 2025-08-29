from fastapi import APIRouter, HTTPException, Depends
from typing import List
import logging
from sqlalchemy.orm import Session

from app.schemas import (
    RecommendMemberRequest, 
    RecommendedMember, 
    MemberExplanation,
    UserTechStack
)
from app.database import get_db
from app.models import Project, ProjectTechStack, User, UserProfile, UserTechStack as UserTechStackModel, TechStack

router = APIRouter()
logger = logging.getLogger(__name__)


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
    experience_text = user["experience"]
    if "10년 이상" in experience_text or "5-10년" in experience_text:
        experience_match = "고급 개발자로 팀 리드 경험 보유"
    elif "3-5년" in experience_text:
        experience_match = "중급 개발자로 안정적인 개발 가능"
    elif "1-3년" in experience_text or "1년 미만" in experience_text:
        experience_match = "주니어 개발자로 빠른 학습 능력 보유"
    else:
        experience_match = f"{experience_text} 개발자로 성장 잠재력 보유"
    
    return MemberExplanation(
        main_reason=main_reason,
        detailed_reasons=detailed_reasons,
        matched_skills=matched_skills,
        growth_opportunities=growth_opportunities,
        simple_explanation=simple_explanation,
        experience_match=experience_match
    )

def get_users_from_db(db: Session) -> List[dict]:
    """실제 DB에서 사용자 데이터를 가져오는 함수"""
    try:
        logger.info("🔍 사용자 데이터 조회 시작...")
        users = db.query(User).join(UserProfile, User.user_id == UserProfile.user_id, isouter=True).all()
        logger.info(f"🔍 DB에서 조회된 총 사용자 수: {len(users)}")
        
        if not users:
            logger.warning("⚠️ 조회된 사용자가 없습니다!")
            return []
        
        users_data = []
        for i, user in enumerate(users):
            logger.info(f"🔄 처리 중: {i+1}/{len(users)} - {user.nickname}")
            
            try:
                # 사용자 기술스택 조회 (올바른 JOIN 조건 사용)
                user_tech_stacks = db.query(UserTechStackModel).join(
                    TechStack, UserTechStackModel.stack_id == TechStack.tech_stack_id
                ).filter(
                    UserTechStackModel.user_id == user.user_id
                ).all()
                logger.info(f"👤 사용자 {user.nickname}({user.user_id})의 기술스택 수: {len(user_tech_stacks)}")
            except Exception as e:
                logger.error(f"❌ {user.nickname}의 기술스택 조회 실패: {e}")
                continue
            
            tech_stacks = [
                {"name": uts.stack.name, "level": uts.skill_level}
                for uts in user_tech_stacks
            ]
            
            if tech_stacks:
                logger.info(f"🛠️ {user.nickname}의 기술스택: {[ts['name'] for ts in tech_stacks]}")
            
            # 경험 범위를 년수로 변환
            experience_range = user.profile.experience_range if user.profile else "FRESHER"
            experience_text = {
                "FRESHER": "신입",
                "LT_1": "1년 미만",
                "Y1_3": "1-3년", 
                "Y3_5": "3-5년",
                "Y5_10": "5-10년",
                "GE_10": "10년 이상",
                "ETC": "기타"
            }.get(experience_range, "1-3년")
            
            # 프로필 이미지 처리 (더 안정적인 fallback)
            profile_image = None
            if user.profile_image and user.profile_image.startswith('http'):
                profile_image = user.profile_image
            else:
                # dicebear API를 사용한 더 다양한 아바타 생성
                profile_image = f"https://api.dicebear.com/7.x/avataaars/svg?seed={user.user_id}&backgroundColor=b6e3f4,c0aede,d1d4f9,ffd5dc,ffdfbf"
            
            user_data = {
                "userId": user.user_id,
                "name": user.nickname,
                "email": user.email,
                "profileImage": profile_image,
                "userTechStacks": tech_stacks,
                "experience": experience_text,
                "portfolioCount": 0,  # 추후 실제 포트폴리오 테이블과 연동
                "completedProjects": 0,  # 추후 실제 프로젝트 완료 데이터와 연동
                "averageRating": float(user.profile.reputation_score) if user.profile and user.profile.reputation_score else 4.0,
                "lastActiveDate": "2025-08-24",  # 추후 실제 활동 데이터와 연동
                "isAvailable": True,
                "currentProjectCount": 0  # 추후 실제 참여 중인 프로젝트 수와 연동
            }
            users_data.append(user_data)
        
        return users_data
        
    except Exception as e:
        logger.error(f"DB에서 사용자 데이터 조회 실패: {e}")
        return []

@router.post("/recommend/members", response_model=List[RecommendedMember])
async def recommend_team_members(request: RecommendMemberRequest, db: Session = Depends(get_db)) -> List[RecommendedMember]:
    """팀원 추천 API"""
    logger.info(f"🤖 팀원 추천 요청 받음")
    logger.info(f"🔍 요청 데이터: {request}")
    logger.info(f"🔍 프로젝트 ID: {request.projectId}, 필요 기술: {request.requiredSkills}, 팀 크기: {request.teamSize}")
    
    try:
        # 1. DB에서 실제 사용자 데이터 조회
        logger.info("DB에서 실제 사용자 데이터를 조회합니다.")
        users_data = get_users_from_db(db)
        logger.info(f"📊 DB에서 {len(users_data)}명의 사용자 데이터 조회")
        
        # DB 조회 결과 확인
        if not users_data:
            logger.warning("DB에서 조회된 사용자가 없습니다.")
            return []
        
        # 2. 매칭 점수 계산 및 정렬
        candidates = []
        logger.info(f"🎯 매칭 대상 기술: {request.requiredSkills}")
        
        for user in users_data:
            # 팀장 본인은 추천에서 제외
            if request.excludeUserId and user["userId"] == request.excludeUserId:
                logger.info(f"❌ {user['name']}({user['userId']}) - 팀장 본인이므로 제외")
                continue
                
            if not user["isAvailable"]:
                logger.info(f"❌ {user['name']} - 참여 불가능 상태")
                continue
            
            user_skills = [skill["name"] for skill in user["userTechStacks"]]
            logger.info(f"👤 {user['name']}의 기술스택: {user_skills}")
                
            match_score = calculate_match_score(user["userTechStacks"], request.requiredSkills)
            logger.info(f"📊 {user['name']} 매칭 점수: {match_score}")
            
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
        
        # 3. 매칭 점수 순으로 정렬
        candidates.sort(key=lambda x: x.matchScore, reverse=True)
        
        # 4. 요청된 팀 크기만큼 반환
        result = candidates[:request.teamSize]
        
        logger.info(f"✅ 팀원 추천 완료 - 추천된 팀원 수: {len(result)}")
        return result
        
    except Exception as e:
        logger.error(f"❌ 팀원 추천 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=f"팀원 추천 중 오류가 발생했습니다: {str(e)}")