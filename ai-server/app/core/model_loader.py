"""
AI 모델 로더 - 싱글톤 패턴으로 모델 관리
"""
import os
import joblib
import logging
from typing import Optional, Any
from pathlib import Path

logger = logging.getLogger(__name__)


class ModelLoader:
    """ML 모델을 로드하고 관리하는 싱글톤 클래스"""
    
    _instance: Optional['ModelLoader'] = None
    _model: Optional[Any] = None
    _model_loaded: bool = False
    
    def __new__(cls) -> 'ModelLoader':
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        if not hasattr(self, '_initialized'):
            self._initialized = True
            self._load_model()
    
    def _load_model(self) -> None:
        """모델을 로드합니다. 실패 시 더미 모델을 사용합니다."""
        from app.config import get_model_path
        
        model_path = Path(get_model_path())
        
        try:
            if model_path.exists():
                self._model = joblib.load(model_path)
                self._model_loaded = True
                logger.info(f"ML 모델을 성공적으로 로드했습니다: {model_path}")
            else:
                logger.warning(f"모델 파일을 찾을 수 없습니다: {model_path}")
                self._create_dummy_model()
        except Exception as e:
            logger.error(f"모델 로드 실패: {e}")
            self._create_dummy_model()
    
    def _create_dummy_model(self) -> None:
        """더미 모델을 생성합니다."""
        class DummyModel:
            def predict_proba(self, features):
                # features[0]이 overlap 점수라고 가정
                overlap = features[0][0] if features and len(features[0]) > 0 else 0.0
                return [[1 - overlap, overlap]]
        
        self._model = DummyModel()
        self._model_loaded = False
        logger.info("더미 모델을 사용합니다.")
    
    @property
    def model(self) -> Any:
        """로드된 모델을 반환합니다."""
        return self._model
    
    @property
    def is_ml_model_loaded(self) -> bool:
        """실제 ML 모델이 로드되었는지 확인합니다."""
        return self._model_loaded
    
    def reload_model(self) -> bool:
        """모델을 다시 로드합니다."""
        try:
            self._load_model()
            return True
        except Exception as e:
            logger.error(f"모델 재로드 실패: {e}")
            return False


def get_model() -> Any:
    """모델 인스턴스를 반환하는 팩토리 함수"""
    return ModelLoader().model


def is_ml_model_available() -> bool:
    """실제 ML 모델이 사용 가능한지 확인"""
    return ModelLoader().is_ml_model_loaded