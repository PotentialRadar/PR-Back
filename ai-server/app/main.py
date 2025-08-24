from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
import os
import logging

from app.router.recommendation_router import router as recommendation_router
from app.router.team_recommendation_router import router as team_recommendation_router
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

# 전역 예외 처리기 추가
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger = logging.getLogger(__name__)
    logger.error(f"❌ 유효성 검사 실패: {exc.errors()}")
    try:
        body = await request.body()
        logger.error(f"❌ 요청 본문: {body.decode('utf-8')}")
        body_str = body.decode('utf-8')
    except Exception as e:
        logger.error(f"❌ 요청 본문 읽기 실패: {e}")
        body_str = "읽기 실패"
    
    return JSONResponse(
        status_code=422,
        content={
            "detail": "유효성 검사 실패",
            "errors": exc.errors(),
            "request_body": body_str
        }
    )

# 라우터 등록
app.include_router(recommendation_router, prefix="/api", tags=["recommendations"])
app.include_router(team_recommendation_router, prefix="/api", tags=["team-recommendations"])


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