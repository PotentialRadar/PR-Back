from fastapi import APIRouter
from typing import List

# 별도의 schemas.py 파일에서 모델을 임포트합니다.
from app.schemas import RecommendRequest, ProjectRecommendation, UserTechStack
from ..service.recommendation_service import get_recommended_projects

router = APIRouter()

@router.post("/recommend/projects", response_model=List[ProjectRecommendation])
def recommend_projects(request: RecommendRequest):  # ✅ RecommendRequest로 수정
    print("🔍 받은 request:", request)
    print("🔍 dict 변환:", request.dict())
    
    return get_recommended_projects(request)
