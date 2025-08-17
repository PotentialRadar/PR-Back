"""
사용자 정의 예외 클래스들
"""
from fastapi import HTTPException
from typing import Optional, Any, Dict


class RecommendationError(Exception):
    """추천 시스템 관련 기본 예외"""
    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        self.message = message
        self.details = details or {}
        super().__init__(self.message)


class ModelLoadError(RecommendationError):
    """모델 로드 관련 예외"""
    pass


class DatabaseConnectionError(RecommendationError):
    """데이터베이스 연결 관련 예외"""
    pass


class InvalidRequestError(RecommendationError):
    """잘못된 요청 관련 예외"""
    pass


def create_http_exception(
    status_code: int,
    message: str,
    details: Optional[Dict[str, Any]] = None
) -> HTTPException:
    """
    HTTPException을 생성하는 헬퍼 함수
    
    Args:
        status_code: HTTP 상태 코드
        message: 에러 메시지
        details: 추가 상세 정보
        
    Returns:
        FastAPI HTTPException
    """
    detail = {"message": message}
    if details:
        detail.update(details)
    
    return HTTPException(status_code=status_code, detail=detail)