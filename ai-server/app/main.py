from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os

from app.router.recommendation_router import router as recommendation_router
from app.router.team_recommendation_router import router as team_recommendation_router
from app.routers.project_recommendation import router as project_recommendation_router
from app.core.logging_config import setup_logging

# 로깅 설정
log_level = os.getenv("LOG_LEVEL", "INFO")
setup_logging(log_level)

# FastAPI 앱 생성
app = FastAPI(
    title="AI Recommendation Server",
    description="프로젝트 추천을 위한 AI 서버",
    version="1.0.0"
)

# CORS 미들웨어 추가
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 구체적인 도메인으로 제한
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
# app.include_router(recommendation_router, prefix="/api", tags=["recommendations"])
app.include_router(team_recommendation_router, prefix="/api", tags=["team-recommendations"])
app.include_router(project_recommendation_router, prefix="/api", tags=["project-recommendations"])


@app.get("/", tags=["health"])
def root():
    """서버 상태 확인 엔드포인트"""
    return {"message": "AI Server is running", "status": "healthy"}


@app.get("/health", tags=["health"])
def health_check():
    """상세 헬스 체크 엔드포인트"""
    from app.core.model_loader import is_ml_model_available
    from app.schemas import HealthResponse
    
    return HealthResponse(
        status="healthy",
        ml_model_loaded=is_ml_model_available(),
        version="1.0.0"
    )