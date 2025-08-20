# app/config.py
import os
from typing import Optional

try:
    # .env 자동 로드 (없으면 조용히 패스)
    from dotenv import load_dotenv
    load_dotenv()
except Exception:
    pass

def _get_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name, str(default)).strip().lower()
    return raw in ("1", "true", "yes", "y", "on")

class Settings:
    # 기본값을 환경변수에서 가져옴 (.env 또는 시스템 env)
    RECO_DEFAULT_TOPN: int = int(os.getenv("RECO_DEFAULT_TOPN", "5"))
    RECO_DEFAULT_MINSCORE: float = float(os.getenv("RECO_DEFAULT_MINSCORE", "0.5"))
    RECO_DEFAULT_MINOVERLAP: float = float(os.getenv("RECO_DEFAULT_MINOVERLAP", "0.2"))
    RECO_DEFAULT_STRICT: bool = _get_bool("RECO_DEFAULT_STRICT", False)
    
    # 모델 경로 설정
    MODEL_DIR: str = os.getenv("MODEL_DIR", "app/model")
    MODEL_FILENAME: str = os.getenv("MODEL_FILENAME", "recommender.pkl")
    
    # 학습 데이터 경로 설정
    TRAINING_DATA_DIR: str = os.getenv("TRAINING_DATA_DIR", ".")
    TRAINING_DATA_FILENAME: str = os.getenv("TRAINING_DATA_FILENAME", "training_data_enhanced.csv")

settings = Settings()

# 모델 경로 유틸리티 함수
def get_model_path() -> str:
    """모델 파일의 절대 경로를 반환합니다."""
    from pathlib import Path
    
    if os.path.isabs(settings.MODEL_DIR):
        # 절대 경로인 경우
        model_dir = Path(settings.MODEL_DIR)
    else:
        # 상대 경로인 경우 (현재 파일 기준)
        current_file = Path(__file__)
        model_dir = current_file.parent / settings.MODEL_DIR
    
    # 디렉토리 존재 보장
    model_dir.mkdir(parents=True, exist_ok=True)
    
    return str(model_dir / settings.MODEL_FILENAME)

def get_training_data_path() -> str:
    """학습 데이터 파일의 절대 경로를 반환합니다."""
    from pathlib import Path
    
    if os.path.isabs(settings.TRAINING_DATA_DIR):
        data_dir = Path(settings.TRAINING_DATA_DIR)
    else:
        # ai-server 루트 디렉토리 기준
        current_file = Path(__file__)
        data_dir = current_file.parent.parent / settings.TRAINING_DATA_DIR
    
    return str(data_dir / settings.TRAINING_DATA_FILENAME)