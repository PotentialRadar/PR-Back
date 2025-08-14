from fastapi import APIRouter, Query, Depends
from typing import List
from sqlalchemy.orm import Session
from app.schemas import RecommendRequest, ProjectRecommendation
from app.utils.feature_engineering import compute_features, final_score, enhanced_final_score
from app.utils.preprocess import normalize_tech_stacks, to_name_list
from app.config import settings
from app.database import get_db
from app.models import Project  # DB 모델 임포트
import joblib
import os

router = APIRouter()

# 모델 로드 (없으면 None) - 향상된 알고리즘 테스트를 위해 일시적으로 비활성화
current_dir = os.path.dirname(__file__)
model_path = os.path.join(current_dir, "../model/recommender.pkl")
try:
    # model = joblib.load(model_path)  # 일시적으로 주석 처리
    model = None  # 향상된 룰 기반 알고리즘 사용을 위해 강제로 None 설정
    print(f"향상된 룰 기반 알고리즘을 사용합니다 (ML 모델 비활성화)")
except Exception as e:
    print(f"모델 로드 실패({model_path}): {e}")
    model = None

@router.post("/recommend/projects", response_model=List[ProjectRecommendation])
def recommend_projects(
    request: RecommendRequest,
    db: Session = Depends(get_db), # DB 세션 의존성 주입
    topN: int = Query(settings.RECO_DEFAULT_TOPN, ge=1, le=50),
    minScore: float = Query(settings.RECO_DEFAULT_MINSCORE, ge=0.0, le=1.0),
    minOverlap: float = Query(settings.RECO_DEFAULT_MINOVERLAP, ge=0.0, le=1.0),
    strict: bool = Query(settings.RECO_DEFAULT_STRICT),
):
    """
    - topN: 상위 N개 보장
    - minScore: 임계값(기본 0.5)
    - minOverlap: 사용자/프로젝트 기술스택 Jaccard 겹침 비율 최소값
    - strict: True면 임계값과 minOverlap 모두 만족하는 항목만 반환(topN 미만이어도 채우지 않음)
                False면 임계값/겹침 통과 결과가 모자라면 점수 상위로 채워서 N개 보장
    """
    return get_recommended_projects(
        request,
        db=db, # 주입받은 db 세션을 전달
        topN=topN,
        min_threshold=minScore,
        min_overlap=minOverlap,
        strict=strict,
    )


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
    
    # 1. DB에서 모든 프로젝트 정보를 가져옵니다.
    # 성능: 프로젝트가 매우 많아지면 필터링 또는 페이지네이션을 고려해야 합니다.
    db_projects = db.query(Project).all()

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

        if model is not None:
            # === ML 경로 ===
            feats = compute_features(user_names, proj_names)  # e.g., [overlap]
            score = float(model.predict_proba([feats])[0][1])
            print(f"[DEBUG] Using ML model for project {p.projectId}")
        else:
            # === 룰 기반 경로 (향상된 알고리즘) ===
            score = float(enhanced_final_score(user_names, user_norm, proj_names, proj_norm))
            print(f"[DEBUG] Using enhanced_final_score for project {p.projectId}")

        print(f"[DEBUG] Project ID {p.projectId}, overlap={overlap:.2f}, score={score:.4f}")

        p.matchScore = round(score, 2)
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

    # 요약 로그 한 줄 (디버그 보기 편하게)
    try:
        summary = [(p.projectId, float(p.matchScore)) for p in result]
        print(
            "[REC] strict=%s topN=%d minScore=%.2f minOverlap=%.2f -> %s"
            % (strict, topN, min_threshold, min_overlap, summary)
        )
    except Exception as _:
        pass

    return result 