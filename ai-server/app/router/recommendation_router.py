from fastapi import APIRouter, Query, Depends, HTTPException
from typing import List
import logging
from sqlalchemy.orm import Session, joinedload
from sqlalchemy.exc import SQLAlchemyError

from app.schemas import RecommendRequest, ProjectRecommendation, RecommendationExplanation
from app.utils.feature_engineering import compute_features, enhanced_final_score
from app.utils.preprocess import normalize_tech_stacks, to_name_list
from app.utils.explanation_generator import RecommendationExplainer
from app.utils.like_analyzer import LikePatternAnalyzer
from app.services.feedback_service import feedback_service
from app.config import settings
from app.database import get_db
from app.models import Project, ProjectTechStack
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
    user_norm = normalize_tech_stacks(tech_stack_dicts, min_level=1, max_level=5, allow_unknown=True)
    user_names = to_name_list(user_norm)
    
    # 개발도구를 실제 기술스택으로 확장
    from app.utils.preprocess import expand_dev_tools_to_tech_stacks
    user_names_expanded = expand_dev_tools_to_tech_stacks(user_names)
    logger.info(f"🔧 개발도구 확장 전: {user_names}")
    logger.info(f"🚀 개발도구 확장 후: {user_names_expanded}")
    user_names = user_names_expanded
    
    logger.info(f"🔍 사용자 기술스택: {user_names}")
    
    # 🆕 피드백 통계 조회 (설명 개인화용)
    user_feedback_stats = None
    try:
        feedback_service.log_feedback_impact(request.user_id)
        user_feedback_stats = feedback_service.get_user_feedback_stats(request.user_id)
        logger.info(f"📊 설명 개인화용 피드백 통계 조회 완료: {user_feedback_stats.get('totalFeedbacks', 0)}개")
    except Exception as e:
        logger.warning(f"⚠️ 피드백 통계 조회 실패: {e}")
        user_feedback_stats = None
    
    # 디버깅: DB에서 로드된 기술스택 확인
    from app.utils.preprocess import get_allowed_tech
    allowed_techs = get_allowed_tech()
    logger.info(f"🔍 DB에서 로드된 기술스택 개수: {len(allowed_techs)}")
    logger.info(f"🔍 사용자 기술 중 DB에 있는 것들: {[tech for tech in user_names if tech in allowed_techs]}")
    logger.info(f"🔍 사용자 기술 중 DB에 없는 것들: {[tech for tech in user_names if tech not in allowed_techs]}")
    
    # 샘플로 DB 기술스택 몇 개 출력
    sample_techs = list(allowed_techs)[:10]
    logger.info(f"🔍 DB 기술스택 샘플: {sample_techs}")
    
    
    # 좋아요 패턴 분석 (새로 추가)
    like_analyzer = LikePatternAnalyzer()
    user_preferences = None
    use_likes = request.include_likes and request.liked_projects
    
    logger.info(f"🔍 좋아요 데이터 확인 - include_likes: {request.include_likes}, liked_projects: {len(request.liked_projects) if request.liked_projects else 0}")
    
    if use_likes:
        user_preferences = like_analyzer.analyze_user_preferences(request.liked_projects)
        logger.info(f"🎯 좋아요 기반 추천 활성화 - {len(request.liked_projects)}개 프로젝트 분석 완료")
        logger.info(f"🎯 사용자 선호 기술: {list(user_preferences.get('preferred_techs', {}).keys())[:5]}")
    else:
        logger.info("📍 좋아요 데이터 사용 안 함 - 기술스택만 사용")
    
    # 1. DB에서 프로젝트 정보를 tech_stacks와 함께 한 번에 가져옵니다 (N+1 쿼리 방지)
    # 성능 최적화: 필요 시 topN * 2 정도로 제한하여 메모리 사용량 최적화
    max_projects = max(topN * 3, 50)  # 최소 50개, 최대 요청량의 3배
    
    try:
        db_projects = (
            db.query(Project)
            .options(joinedload(Project.tech_stacks).joinedload(ProjectTechStack.tech_stack))
            .limit(max_projects)
            .all()
        )
        logger.debug(f"DB에서 {len(db_projects)}개 프로젝트 조회 (제한: {max_projects})")
    except Exception as e:
        logger.error(f"프로젝트 조회 중 오류: {e}")
        raise HTTPException(status_code=503, detail="프로젝트 데이터 조회 실패")

    # 2. DB 모델 객체(Project)를 Pydantic 스키마(ProjectRecommendation)로 변환합니다.
    all_projects = []
    for p in db_projects:
        tech_stacks = [ts.tech_stack.name for ts in p.tech_stacks if ts.tech_stack]
        logger.debug(f"🔍 프로젝트 {p.project_id} ({p.title}): {tech_stacks}")
        
        all_projects.append(ProjectRecommendation(
            projectId=p.project_id,
            title=p.title,
            description=p.description,
            matchScore=0.0, # 점수는 나중에 계산
            projectTechStacks=tech_stacks
        ))
    
    logger.info(f"📊 총 {len(all_projects)}개 프로젝트 변환 완료")
    
    # 프로젝트에서 실제 사용되는 기술스택 확인
    project_techs_sample = []
    for p in all_projects[:3]:  # 처음 3개 프로젝트만
        logger.info(f"🔍 프로젝트 {p.projectId} 기술스택: {p.projectTechStacks}")
        project_techs_sample.extend(p.projectTechStacks[:3])  # 각 프로젝트에서 3개씩
    logger.info(f"🔍 실제 프로젝트들이 사용하는 기술스택 샘플: {project_techs_sample[:10]}")

    scored: List[ProjectRecommendation] = []
    explainer = RecommendationExplainer()

    for p in all_projects:
        # 프로젝트 스택도 정규화하여 공정 비교
        proj_raw = [{"name": name, "level": 3} for name in p.projectTechStacks]
        proj_norm = normalize_tech_stacks(proj_raw, min_level=1, max_level=5, allow_unknown=True)
        proj_names = to_name_list(proj_norm)
        
        logger.debug(f"프로젝트 {p.projectId} 기술스택: {proj_names}")

        # Jaccard overlap for transparent gating
        u_set = set(user_names)
        p_set = set(proj_names)
        union = u_set | p_set
        overlap = (len(u_set & p_set) / len(union)) if union else 0.0

        model = get_model()
        
        if is_ml_model_available():
            # ML 모델 사용
            feats = compute_features(user_names, proj_names)
            tech_score = float(model.predict_proba([feats])[0][1])
            logger.debug(f"ML 모델 사용 - 프로젝트 {p.projectId}")
        else:
            # 룰 기반 알고리즘 사용
            tech_score = float(enhanced_final_score(user_names, user_norm, proj_names, proj_norm))
            logger.debug(f"룰 기반 알고리즘 사용 - 프로젝트 {p.projectId}")
        
        # 좋아요 기반 점수 계산 및 통합 (새로 추가)
        if use_likes and user_preferences:
            like_score = like_analyzer.calculate_like_similarity_score(
                user_preferences, 
                p.projectTechStacks,
                "기타"  # 카테고리는 추후 프로젝트 도메인에서 가져올 수 있음
            )
            # 하이브리드 점수: 기술스택 70% + 좋아요 패턴 30%
            score = (tech_score * 0.7) + (like_score * 0.3)
            logger.debug(f"하이브리드 점수 - 프로젝트 {p.projectId}: tech={tech_score:.3f}, like={like_score:.3f}, final={score:.3f}")
        else:
            score = tech_score
            logger.debug(f"기술스택 점수만 사용 - 프로젝트 {p.projectId}: {score:.3f}")
        
        # 🆕 피드백 기반 점수 조정
        try:
            adjusted_score = feedback_service.adjust_recommendation_score(
                base_score=score,
                user_id=request.user_id,
                project_tech_stacks=p.projectTechStacks
            )
            if adjusted_score != score:
                logger.info(f"🎯 피드백 조정 - 프로젝트 {p.projectId}: {score:.3f} → {adjusted_score:.3f}")
            score = adjusted_score
        except Exception as e:
            logger.warning(f"⚠️ 피드백 조정 실패 - 프로젝트 {p.projectId}: {e}")
            # 피드백 조정 실패해도 기본 점수는 유지

        logger.debug(f"🔍 점수 계산 - 프로젝트 {p.projectId} ({p.title}): overlap={overlap:.2f}, tech_score={tech_score:.4f}, final_score={score:.4f}")
        logger.debug(f"  사용자 기술: {user_names}")
        logger.debug(f"  프로젝트 기술: {proj_names}")
        logger.debug(f"  교집합: {u_set & p_set}")

        # Explanation 생성
        try:
            logger.debug(f"설명 생성 시작 - 프로젝트 {p.projectId}: {p.title}")
            logger.debug(f"사용자 기술: {user_names}")
            logger.debug(f"프로젝트 기술: {proj_names}")
            
            explanation_data = explainer.generate_explanation(
                user_techs=user_names,
                user_norm=user_norm,
                project_techs=proj_names,
                project_norm=proj_norm,
                project_title=p.title,
                match_score=score,
                view_count=None,  # TODO: 조회수 데이터 추가 시 활용
                user_feedback_stats=user_feedback_stats  # 🆕 피드백 기반 개인화
            )
            
            logger.debug(f"설명 데이터 생성됨: {explanation_data}")
            explanation = RecommendationExplanation(**explanation_data)
            logger.debug(f"설명 객체 생성 완료: {explanation}")
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