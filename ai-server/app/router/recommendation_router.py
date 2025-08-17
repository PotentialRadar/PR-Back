from fastapi import APIRouter, Query, Depends, HTTPException
from typing import List
import logging
from sqlalchemy.orm import Session, joinedload
from sqlalchemy.exc import SQLAlchemyError

from app.schemas import RecommendRequest, ProjectRecommendation, RecommendationExplanation
from app.utils.feature_engineering import compute_features, enhanced_final_score
from app.utils.preprocess import normalize_tech_stacks, to_name_list
from app.utils.explanation_generator import RecommendationExplainer
from app.config import settings
from app.database import get_db
from app.models import Project
from app.core.model_loader import get_model, is_ml_model_available

logger = logging.getLogger(__name__)
router = APIRouter()

@router.post("/recommend/projects", response_model=List[ProjectRecommendation])
def recommend_projects(
    request: RecommendRequest,
    db: Session = Depends(get_db),
    topN: int = Query(settings.RECO_DEFAULT_TOPN, ge=1, le=50),
    minScore: float = Query(settings.RECO_DEFAULT_MINSCORE, ge=0.0, le=1.0),
    minOverlap: float = Query(settings.RECO_DEFAULT_MINOVERLAP, ge=0.0, le=1.0),
    strict: bool = Query(settings.RECO_DEFAULT_STRICT),
):
    """
    프로젝트 추천 API
    
    Args:
        request: 사용자 기술스택 정보
        topN: 상위 N개 추천 (1-50)
        minScore: 최소 점수 임계값 (0.0-1.0)
        minOverlap: 최소 기술스택 겹침 비율 (0.0-1.0)
        strict: 엄격 모드 (True시 임계값 미만 제외)
    
    Returns:
        추천 프로젝트 리스트 (점수순 정렬)
    """
    try:
        # 입력 검증
        if not request.tech_stacks:
            raise HTTPException(status_code=400, detail="기술스택이 비어있습니다")
        
        return get_recommended_projects(
            request=request,
            db=db,
            topN=topN,
            min_threshold=minScore,
            min_overlap=minOverlap,
            strict=strict,
        )
    except HTTPException:
        # 이미 처리된 HTTP 예외는 그대로 전파
        raise
    except SQLAlchemyError as e:
        logger.error(f"데이터베이스 오류: {e}")
        raise HTTPException(status_code=503, detail="데이터베이스 연결 오류")
    except Exception as e:
        logger.error(f"추천 시스템 오류: {e}")
        raise HTTPException(status_code=500, detail="추천 시스템 내부 오류")


def get_recommended_projects(
    request: RecommendRequest,
    db: Session, # DB 세션을 파라미터로 받음
    topN: int = 5,
    min_threshold: float = 0.5,
    min_overlap: float = 0.2,
    strict: bool = False,
) -> List[ProjectRecommendation]:
    # 전처리: 요청 기술스택 정규화
    tech_stack_dicts = [ts.dict() for ts in request.tech_stacks]
    user_norm = normalize_tech_stacks(tech_stack_dicts, min_level=1, max_level=5)
    user_names = to_name_list(user_norm)
    
    # 1. DB에서 프로젝트 정보를 tech_stacks와 함께 한 번에 가져옵니다 (N+1 쿼리 방지)
    # 성능 최적화: 필요 시 topN * 2 정도로 제한하여 메모리 사용량 최적화
    max_projects = max(topN * 3, 50)  # 최소 50개, 최대 요청량의 3배
    
    try:
        db_projects = (
            db.query(Project)
            .options(joinedload(Project.tech_stacks))
            .limit(max_projects)
            .all()
        )
        logger.debug(f"DB에서 {len(db_projects)}개 프로젝트 조회 (제한: {max_projects})")
    except Exception as e:
        logger.error(f"프로젝트 조회 중 오류: {e}")
        raise HTTPException(status_code=503, detail="프로젝트 데이터 조회 실패")

    # 2. DB 모델 객체(Project)를 Pydantic 스키마(ProjectRecommendation)로 변환합니다.
    all_projects = [
        ProjectRecommendation(
            projectId=p.project_id,
            title=p.title,
            description=p.description,
            matchScore=0.0, # 점수는 나중에 계산
            projectTechStacks=[ts.tech_stack_name for ts in p.tech_stacks] # 수정된 모델의 컬럼명(tech_stack_name)으로 변경
        )
        for p in db_projects
    ]

    scored: List[ProjectRecommendation] = []
    explainer = RecommendationExplainer()

    for p in all_projects:
        # 프로젝트 스택도 정규화하여 공정 비교
        proj_raw = [{"name": name, "level": 3} for name in p.projectTechStacks]
        proj_norm = normalize_tech_stacks(proj_raw, min_level=1, max_level=5)
        proj_names = to_name_list(proj_norm)

        # Jaccard overlap for transparent gating
        u_set = set(user_names)
        p_set = set(proj_names)
        union = u_set | p_set
        overlap = (len(u_set & p_set) / len(union)) if union else 0.0

        model = get_model()
        
        if is_ml_model_available():
            # ML 모델 사용
            feats = compute_features(user_names, proj_names)
            score = float(model.predict_proba([feats])[0][1])
            logger.debug(f"ML 모델 사용 - 프로젝트 {p.projectId}")
        else:
            # 룰 기반 알고리즘 사용
            score = float(enhanced_final_score(user_names, user_norm, proj_names, proj_norm))
            logger.debug(f"룰 기반 알고리즘 사용 - 프로젝트 {p.projectId}")

        logger.debug(f"프로젝트 {p.projectId}: overlap={overlap:.2f}, score={score:.4f}")

        # Explanation 생성
        try:
            logger.info(f"설명 생성 시작 - 프로젝트 {p.projectId}: {p.title}")
            logger.info(f"사용자 기술: {user_names}")
            logger.info(f"프로젝트 기술: {proj_names}")
            
            explanation_data = explainer.generate_explanation(
                user_techs=user_names,
                user_norm=user_norm,
                project_techs=proj_names,
                project_norm=proj_norm,
                project_title=p.title,
                match_score=score
            )
            
            logger.info(f"설명 데이터 생성됨: {explanation_data}")
            explanation = RecommendationExplanation(**explanation_data)
            logger.info(f"설명 객체 생성 완료: {explanation}")
        except Exception as e:
            logger.error(f"설명 생성 실패 (프로젝트 {p.projectId}): {e}")
            logger.error(f"예외 상세: {type(e).__name__}: {str(e)}")
            import traceback
            logger.error(f"트레이스백: {traceback.format_exc()}")
            explanation = None

        p.matchScore = round(score, 2)
        p.explanation = explanation
        # keep overlap for post-filtering (not serialized)
        p.__dict__["_overlap"] = overlap
        scored.append(p)

    # 점수순 정렬 (내림차순)
    scored.sort(key=lambda x: x.matchScore, reverse=True)

    # 1) 임계값 + 겹침 통과 항목 우선 선별
    passing = [
        p for p in scored
        if p.matchScore >= min_threshold and getattr(p, "_overlap", 0.0) >= min_overlap
    ]

    if strict:
        # strict 모드면 통과 항목만, 부족해도 채우지 않음
        result = passing[:topN]
    else:
        # 느슨 모드: 통과 항목이 충분하면 그 중 상위 N개, 아니면 전체에서 상위 N개 보장
        result = passing[:topN] if len(passing) >= topN else scored[:topN]

    # 추천 결과 로깅
    try:
        summary = [(p.projectId, float(p.matchScore)) for p in result]
        logger.info(
            f"추천 완료 - strict={strict}, topN={topN}, "
            f"minScore={min_threshold:.2f}, minOverlap={min_overlap:.2f} -> {summary}"
        )
    except Exception as e:
        logger.warning(f"추천 결과 로깅 실패: {e}")

    return result 