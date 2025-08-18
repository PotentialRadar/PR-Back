"""
특성 엔지니어링 및 점수 계산 유틸리티
"""
from typing import List, Dict, Optional
from math import sqrt
import logging

# tech_similarity 모듈을 직접 구현

logger = logging.getLogger(__name__)

def calculate_tech_similarity(user_techs: List[str], project_techs: List[str]) -> float:
    """
    간단한 기술스택 연관성 점수를 계산합니다.
    
    Args:
        user_techs: 사용자 기술스택
        project_techs: 프로젝트 기술스택
        
    Returns:
        연관성 점수 (0.0 ~ 1.0)
    """
    if not user_techs or not project_techs:
        return 0.0
    
    # 기본적인 기술스택 그룹 정의
    tech_groups = {
        'frontend': ['React', 'Vue.js', 'Angular', 'JavaScript', 'TypeScript', 'HTML', 'CSS'],
        'backend': ['Node.js', 'Python', 'Java', 'Spring', 'Django', 'FastAPI', 'Express'],
        'mobile': ['Flutter', 'React Native', 'iOS', 'Android', 'Swift', 'Kotlin'],
        'database': ['PostgreSQL', 'MongoDB', 'MySQL', 'Redis'],
        'cloud': ['AWS', 'Docker', 'Kubernetes', 'Azure', 'GCP'],
        'ai': ['TensorFlow', 'PyTorch', 'Machine Learning', 'OpenCV']
    }
    
    # 각 기술이 속한 그룹 찾기
    user_groups = set()
    project_groups = set()
    
    for tech in user_techs:
        for group, techs in tech_groups.items():
            if tech in techs:
                user_groups.add(group)
    
    for tech in project_techs:
        for group, techs in tech_groups.items():
            if tech in techs:
                project_groups.add(group)
    
    # 공통 그룹 비율 계산
    if not user_groups or not project_groups:
        return 0.0
    
    common_groups = user_groups & project_groups
    total_groups = user_groups | project_groups
    
    return len(common_groups) / len(total_groups) if total_groups else 0.0

# === (A) ML용 특성: 겹침 비율 1개만 ===
def compute_features(user_techs: List[str], project_techs: List[str]) -> List[float]:
    """
    ML 모델용 특성 벡터를 계산합니다.
    
    Args:
        user_techs: 사용자 기술스택 리스트
        project_techs: 프로젝트 기술스택 리스트
        
    Returns:
        특성 벡터 [Jaccard overlap]
    """
    if not user_techs or not project_techs:
        logger.debug("빈 기술스택으로 인해 특성 벡터를 0.0으로 설정")
        return [0.0]
        
    u_set, p_set = set(user_techs), set(project_techs)
    intersection = len(u_set & p_set)
    union = len(u_set | p_set)
    
    jaccard = intersection / union if union > 0 else 0.0
    logger.debug(f"Jaccard 유사도 계산: {intersection}/{union} = {jaccard:.4f}")
    
    return [jaccard]

# === (B) 룰 기반 점수 계산 ===
# jaccard: 단순 겹침비율
def jaccard(user_techs: List[str], project_techs: List[str]) -> float:
    """
    Jaccard 유사도를 계산합니다.
    
    Args:
        user_techs: 사용자 기술스택
        project_techs: 프로젝트 기술스택
        
    Returns:
        Jaccard 유사도 (0.0 ~ 1.0)
    """
    if not user_techs or not project_techs:
        return 0.0
        
    u_set, p_set = set(user_techs), set(project_techs)
    intersection = len(u_set & p_set)
    union = len(u_set | p_set)
    
    return intersection / union if union > 0 else 0.0

#  weighted_overlap: 레벨을 가중치로 쓰는 코사인 유사도 느낌
def weighted_overlap(
    user_norm: List[Dict[str, any]],
    proj_norm: List[Dict[str, any]],
) -> float:
    """
    레벨을 고려한 가중 코사인 유사도를 계산합니다.
    
    Args:
        user_norm: 정규화된 사용자 기술스택 [{"name": str, "level": int}]
        proj_norm: 정규화된 프로젝트 기술스택 [{"name": str, "level": int}]
        
    Returns:
        가중 코사인 유사도 (0.0 ~ 1.0)
    """
    if not user_norm or not proj_norm:
        return 0.0
        
    try:
        user_dict = {x["name"]: int(x.get("level", 3)) for x in user_norm}
        proj_dict = {x["name"]: int(x.get("level", 3)) for x in proj_norm}
        
        common_techs = set(user_dict.keys()) & set(proj_dict.keys())
        if not common_techs:
            return 0.0
            
        numerator = sum(min(user_dict[tech], proj_dict[tech]) for tech in common_techs)
        
        user_magnitude = sqrt(sum(level * level for level in user_dict.values()))
        proj_magnitude = sqrt(sum(level * level for level in proj_dict.values()))
        denominator = user_magnitude * proj_magnitude
        
        return numerator / denominator if denominator > 0 else 0.0
        
    except (KeyError, TypeError, ValueError) as e:
        logger.warning(f"가중 겹침 계산 중 오류: {e}")
        return 0.0

# final_score: 위 두 점수를 0.6/0.4로 섞은 최종 점수
def final_score(
    user_names: List[str],
    user_norm: List[Dict[str, any]],
    proj_names: List[str],
    proj_norm: List[Dict[str, any]],
) -> float:
    """
    기본 하이브리드 점수를 계산합니다.
    
    Args:
        user_names: 사용자 기술스택 이름 리스트
        user_norm: 정규화된 사용자 기술스택
        proj_names: 프로젝트 기술스택 이름 리스트
        proj_norm: 정규화된 프로젝트 기술스택
        
    Returns:
        최종 점수 (0.0 ~ 1.0)
    """
    jaccard_score = jaccard(user_names, proj_names)
    weighted_score = weighted_overlap(user_norm, proj_norm)
    
    # 가중치: 60% Jaccard + 40% 가중 코사인
    final = 0.6 * jaccard_score + 0.4 * weighted_score
    
    logger.debug(f"점수 계산 - Jaccard: {jaccard_score:.4f}, 가중: {weighted_score:.4f}, 최종: {final:.4f}")
    
    return round(final, 4)

# enhanced_final_score: 기술스택 연관성을 고려한 개선된 점수 계산
def enhanced_final_score(
    user_names: List[str],
    user_norm: List[Dict[str, any]],
    proj_names: List[str],
    proj_norm: List[Dict[str, any]],
) -> float:
    """
    기술스택 연관성을 고려한 향상된 점수를 계산합니다.
    
    가중치 구성:
    - 40% Jaccard 유사도
    - 30% 가중 코사인 유사도
    - 30% 기술스택 연관성 점수
    
    Args:
        user_names: 사용자 기술스택 이름 리스트
        user_norm: 정규화된 사용자 기술스택
        proj_names: 프로젝트 기술스택 이름 리스트
        proj_norm: 정규화된 프로젝트 기술스택
        
    Returns:
        향상된 최종 점수 (0.0 ~ 1.0)
    """
    try:
        # 각 점수 계산
        jaccard_score = jaccard(user_names, proj_names)
        weighted_score = weighted_overlap(user_norm, proj_norm)
        # 간단한 기술스택 연관성 점수로 대체
        similarity_score = calculate_tech_similarity(user_names, proj_names)
        
        # 가중치 적용
        final = 0.4 * jaccard_score + 0.3 * weighted_score + 0.3 * similarity_score
        
        logger.debug(
            f"향상된 점수 계산 - Jaccard: {jaccard_score:.4f}, "
            f"가중: {weighted_score:.4f}, 연관성: {similarity_score:.4f}, "
            f"최종: {final:.4f}"
        )
        
        return round(final, 4)
        
    except Exception as e:
        logger.error(f"향상된 점수 계산 중 오류: {e}")
        # 기본 점수로 폴백
        return final_score(user_names, user_norm, proj_names, proj_norm)