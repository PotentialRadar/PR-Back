from fastapi import APIRouter, Query
from typing import List
from app.schemas import RecommendRequest, ProjectRecommendation
from app.utils.feature_engineering import compute_features, final_score
from app.utils.preprocess import normalize_tech_stacks, to_name_list
from app.config import settings
import joblib
import os

router = APIRouter()

# 모델 로드 (없으면 None)
current_dir = os.path.dirname(__file__)
model_path = os.path.join(current_dir, "../model/recommender.pkl")
try:
    model = joblib.load(model_path)
    print(f"모델을 성공적으로 로드했습니다: {model_path}")
except Exception as e:
    print(f"모델 로드 실패({model_path}): {e}")
    model = None

@router.post("/recommend/projects", response_model=List[ProjectRecommendation])
def recommend_projects(
    request: RecommendRequest,
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
        topN=topN,
        min_threshold=minScore,
        min_overlap=minOverlap,
        strict=strict,
    )


def get_recommended_projects(
    request: RecommendRequest,
    topN: int = 5,
    min_threshold: float = 0.5,
    min_overlap: float = 0.2,
    strict: bool = False,
) -> List[ProjectRecommendation]:
    # 전처리: 요청 기술스택 정규화
    tech_stack_dicts = [ts.dict() for ts in request.tech_stacks]
    user_norm = normalize_tech_stacks(tech_stack_dicts, min_level=1, max_level=5)
    user_names = to_name_list(user_norm)

    # 임시 프로젝트 데이터 (예시)
    all_projects = [
        ProjectRecommendation(
            projectId=1,
            title="AI 프로젝트",
            description="AI 추천 시스템 개발",
            matchScore=0.0,
            projectTechStacks=["Spring", "Vue.js"],
        ),
        ProjectRecommendation(
            projectId=2,
            title="웹 개발",
            description="프론트엔드 및 백엔드 개발",
            matchScore=0.0,
            projectTechStacks=["React", "Node.js"],
        ),
        ProjectRecommendation(
            projectId=3,
            title="Spring 백엔드 마이그레이션",
            description="Spring Boot + JPA 기반 모놀리식 개편",
            matchScore=0.0,
            projectTechStacks=["Spring", "PostgreSQL", "Docker"],
        ),
        ProjectRecommendation(
            projectId=4,
            title="Vue 프론트 리뉴얼",
            description="Vue.js + Pinia 상태관리 적용",
            matchScore=0.0,
            projectTechStacks=["Vue.js", "TypeScript"],
        ),
        ProjectRecommendation(
            projectId=5,
            title="React 대시보드",
            description="React + Chart 구현",
            matchScore=0.0,
            projectTechStacks=["React", "TypeScript", "Redis"],
        ),
        ProjectRecommendation(
            projectId=6,
            title="Node.js API 서버",
            description="Express 기반 REST API",
            matchScore=0.0,
            projectTechStacks=["Node.js", "PostgreSQL"],
        ),
        ProjectRecommendation(
            projectId=7,
            title="Python ETL 파이프라인",
            description="Pandas 기반 데이터 적재/변환",
            matchScore=0.0,
            projectTechStacks=["Python", "PostgreSQL", "Docker"],
        ),
        ProjectRecommendation(
            projectId=8,
            title="FastAPI 추천 서비스",
            description="FastAPI + Uvicorn 운영",
            matchScore=0.0,
            projectTechStacks=["Python", "FastAPI", "Redis"],
        ),
        ProjectRecommendation(
            projectId=9,
            title="검색 성능 개선",
            description="OpenSearch 도입 및 인덱스 튜닝",
            matchScore=0.0,
            projectTechStacks=["Python", "Docker", "Redis"],
        ),
        ProjectRecommendation(
            projectId=10,
            title="배치 파이프라인 컨테이너화",
            description="Docker 기반 CI/CD",
            matchScore=0.0,
            projectTechStacks=["Docker", "Python"],
        ),
        # --- 추가 항목 (partial-overlap, alias 등) ---
        ProjectRecommendation(
            projectId=11,
            title="Spring + React 마이그레이션",
            description="Spring 백엔드와 React 프론트 통합 작업",
            matchScore=0.0,
            projectTechStacks=["Spring", "React"],
        ),
        ProjectRecommendation(
            projectId=12,
            title="Vue + Node + Redis 캐시 적용",
            description="Vue.js 프론트와 Node 백엔드, Redis 캐시 도입",
            matchScore=0.0,
            projectTechStacks=["Vue.js", "Node", "Redis"],  # 'Node' 별칭 → node.js 로 표준화 기대
        ),
        ProjectRecommendation(
            projectId=13,
            title="DB 마이그레이션",
            description="Postgre 로 데이터 이전",
            matchScore=0.0,
            projectTechStacks=["Postgre"],  # 별칭 → postgresql 로 표준화 기대
        ),
        ProjectRecommendation(
            projectId=14,
            title="Next.js 대시보드",
            description="Next.js + TypeORM 기반 관리 화면",
            matchScore=0.0,
            projectTechStacks=["Next.js", "TypeORM"],
        ),
        ProjectRecommendation(
            projectId=15,
            title="Spring Boot + JPA 리팩토링",
            description="Spring Boot와 JPA로 레거시 리팩토링",
            matchScore=0.0,
            projectTechStacks=["Spring Boot", "JPA"],  # springboot 별칭 매핑 확인용
        ),
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
        else:
            # === 룰 기반 경로 ===
            score = float(final_score(user_names, user_norm, proj_names, proj_norm))

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