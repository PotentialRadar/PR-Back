# app/utils/feature_engineering.py
from typing import List, Dict
from math import sqrt
from .tech_similarity import enhanced_similarity_score, symmetric_enhanced_similarity

# === (A) ML용 특성: 겹침 비율 1개만 ===
def compute_features(user_techs: List[str], project_techs: List[str]) -> List[float]:
    """
    ML 모델 입력 특성 벡터. 현재는 [Jaccard overlap] 1개만 사용.
    """
    u, p = set(user_techs), set(project_techs)
    if not u or not p:
        return [0.0]
    inter = len(u & p)
    union = len(u | p)
    return [inter / union if union else 0.0]

# === (B) 룰 기반 점수 계산 ===
# jaccard: 단순 겹침비율
def jaccard(user: List[str], proj: List[str]) -> float:
    if not user or not proj:
        return 0.0
    u, p = set(user), set(proj)
    inter = len(u & p)
    union = len(u | p)
    return inter / union if union else 0.0

#  weighted_overlap: 레벨을 가중치로 쓰는 코사인 유사도 느낌
def weighted_overlap(
    user_norm: List[Dict],  # [{"name":..., "level":...}]
    proj_norm: List[Dict],
) -> float:
    if not user_norm or not proj_norm:
        return 0.0
    u = {x["name"]: int(x.get("level", 3)) for x in user_norm}
    p = {x["name"]: int(x.get("level", 3)) for x in proj_norm}
    common = set(u) & set(p)
    if not common:
        return 0.0
    num = sum(min(u[k], p[k]) for k in common)
    den = sqrt(sum(v*v for v in u.values())) * sqrt(sum(v*v for v in p.values()))
    return num / den if den else 0.0

# final_score: 위 두 점수를 0.6/0.4로 섞은 최종 점수
def final_score(
    user_names: List[str],
    user_norm: List[Dict],
    proj_names: List[str],
    proj_norm: List[Dict],
) -> float:
    # 하이브리드: 0.6 * Jaccard + 0.4 * 가중 코사인 유사도
    s1 = jaccard(user_names, proj_names)
    s2 = weighted_overlap(user_norm, proj_norm)
    return round(0.6 * s1 + 0.4 * s2, 4)

# enhanced_final_score: 기술스택 연관성을 고려한 개선된 점수 계산
def enhanced_final_score(
    user_names: List[str],
    user_norm: List[Dict],
    proj_names: List[str],
    proj_norm: List[Dict],
) -> float:
    """
    기술스택 연관성을 고려한 향상된 최종 점수
    
    조합:
    - 40% Jaccard (기본 교집합/합집합)
    - 30% 가중 코사인 유사도 (레벨 고려)  
    - 30% 기술스택 연관성 점수 (새로 추가)
    """
    # 기존 점수들
    s1 = jaccard(user_names, proj_names)  # 기본 Jaccard
    s2 = weighted_overlap(user_norm, proj_norm)  # 가중 코사인
    
    # 새로운 연관성 점수
    s3 = symmetric_enhanced_similarity(user_names, proj_names)
    
    # 가중치 조합: 40% + 30% + 30%
    final = 0.4 * s1 + 0.3 * s2 + 0.3 * s3
    
    return round(final, 4)