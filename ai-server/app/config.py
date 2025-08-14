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

settings = Settings()