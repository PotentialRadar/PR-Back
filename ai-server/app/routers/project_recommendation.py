from fastapi import APIRouter, HTTPException
from typing import List
import logging

from app.schemas import RecommendProjectRequest, RecommendedProject
from app.services.project_recommendation_service import project_recommendation_service

# 로거 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

router = APIRouter()

@router.post("/recommend/projects", response_model=List[RecommendedProject])
async def recommend_projects(
    request: RecommendProjectRequest,
    top_n: int = 5,
    min_score: float = 0.0,
    min_overlap: float = 0.1,
    strict: bool = False
):
    """
    사용자 기술스택을 기반으로 프로젝트 추천
    
    Args:
        request: 사용자 추천 요청 (userId, techStacks, experienceLevel 등)
        top_n: 반환할 최대 추천 개수
        min_score: 최소 매칭 점수 (0.0-1.0)
        min_overlap: 최소 기술스택 겹침 비율
        strict: 엄격한 매칭 모드
    
    Returns:
        추천 프로젝트 목록 (점수 순으로 정렬)
    """
    try:
        logger.info(f"🎯 프로젝트 추천 요청 - 사용자 ID: {request.userId}")
        logger.info(f"📋 기술스택: {[f'{tech.name}(Lv.{tech.level})' for tech in request.techStacks]}")
        logger.info(f"⚙️ 파라미터: top_n={top_n}, min_score={min_score}, strict={strict}")
        
        if not request.techStacks:
            logger.warning("⚠️ 기술스택이 비어있음")
            raise HTTPException(status_code=400, detail="기술스택 정보가 필요합니다")
        
        # 추천 실행
        recommendations = project_recommendation_service.recommend_projects(
            request=request,
            top_n=top_n,
            min_score=min_score
        )
        
        logger.info(f"✅ 추천 완료 - {len(recommendations)}개 프로젝트 추천")
        
        # 추천 결과 로그
        for i, rec in enumerate(recommendations, 1):
            logger.info(f"  {i}. {rec.title} (점수: {rec.matchScore:.3f})")
            if rec.explanation:
                logger.info(f"     💡 {rec.explanation.simple_explanation}")
        
        return recommendations
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ 프로젝트 추천 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"추천 처리 중 오류가 발생했습니다: {str(e)}")

@router.get("/projects/popular", response_model=List[RecommendedProject])
async def get_popular_projects(limit: int = 5):
    """
    인기 프로젝트 조회 (비로그인 사용자용)
    
    Args:
        limit: 반환할 프로젝트 개수
    
    Returns:
        인기 프로젝트 목록 (조회수 순으로 정렬)
    """
    try:
        logger.info(f"🔥 인기 프로젝트 조회 요청 - 상위 {limit}개")
        
        # 임시로 조회수 기준 상위 프로젝트 반환
        popular_projects = sorted(
            project_recommendation_service.mock_projects,
            key=lambda x: x["viewCount"],
            reverse=True
        )[:limit]
        
        result = []
        for project in popular_projects:
            result.append(RecommendedProject(
                projectId=project["projectId"],
                title=project["title"],
                description=project["description"],
                matchScore=0.0,  # 인기 프로젝트는 매칭 점수 없음
                projectTechStacks=project["techStacks"],
                status=project["status"],
                recruitDeadline=project["recruitDeadline"],
                startDate=project["startDate"],
                endDate=project["endDate"],
                recruitCount=project["recruitCount"],
                appliedCount=project["appliedCount"],
                viewCount=project["viewCount"],
                explanation=None  # 인기 프로젝트는 설명 없음
            ))
        
        logger.info(f"✅ 인기 프로젝트 조회 완료 - {len(result)}개")
        return result
        
    except Exception as e:
        logger.error(f"❌ 인기 프로젝트 조회 중 오류 발생: {str(e)}")
        raise HTTPException(status_code=500, detail=f"인기 프로젝트 조회 중 오류가 발생했습니다: {str(e)}")