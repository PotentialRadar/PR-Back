import logging
import sys
from typing import Optional

def setup_logging(log_level: str = "INFO") -> None:
    """로깅 설정을 초기화합니다."""
    
    # 로그 레벨 설정
    level = getattr(logging, log_level.upper(), logging.INFO)
    
    # 로그 포매터 설정
    formatter = logging.Formatter(
        fmt="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )
    
    # 루트 로거 설정
    root_logger = logging.getLogger()
    root_logger.setLevel(level)
    
    # 기존 핸들러 제거
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)
    
    # 콘솔 핸들러 추가
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    console_handler.setLevel(level)
    root_logger.addHandler(console_handler)
    
    # uvicorn 로거 설정
    uvicorn_logger = logging.getLogger("uvicorn")
    uvicorn_logger.setLevel(level)
    
    # FastAPI 앱 로거 설정
    app_logger = logging.getLogger("app")
    app_logger.setLevel(level)
    
    logging.info(f"로깅 설정 완료 - 레벨: {log_level}")