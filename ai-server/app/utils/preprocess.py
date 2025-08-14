from typing import List, Dict, Tuple
import os

# Toggle debug prints via env: AI_DEBUG=true/1/yes
DEBUG = os.getenv("AI_DEBUG", "false").lower() in ("1", "true", "yes")

def dprint(msg: str) -> None:
    if DEBUG:
        print(msg)

# 기존 ALIAS_MAP 확장
ALIAS_MAP = {
    "vue": "vue.js",
    "vuejs": "vue.js",
    "react.js": "react",
    "reactjs": "react",
    "node": "node.js",
    "express.js": "node.js",
    "postgres": "postgresql",
    "postgre": "postgresql",
    "docker-compose": "docker",
    "fast api": "fastapi",
    "py": "python",
    "k8s": "kubernetes",   # Kubernetes 약어
    "springboot": "spring boot",
    "jpa": "spring data jpa",
    # 필요한 약어/표기 계속 추가
}

# ALLOWED_TECH도 팀에서 실제 쓰는 스택 추가
ALLOWED_TECH = {
    "spring", "spring boot", "spring data jpa",
    "vue.js", "react", "node.js", "express",
    "python", "fastapi", "redis", "docker", "kubernetes",
    "postgresql", "mysql", "mariadb", "mongodb",
    "typescript", "javascript",
    "next.js", "nestjs", "opensearch", "elasticsearch",
    "sse", "websocket"
}


def _canonicalize(name: str) -> str:
    """
    기술 스택 이름을 표준화하는 내부 함수.
    """
    # 디버그 1: _canonicalize 함수의 입력값을 확인
    dprint(f"[DEBUG] _canonicalize input: '{name}'")
    
    if not name:
        return ""
    
    # 소문자로 변환, 공백/하이픈/언더바 제거
    n = name.strip().lower().replace("_", " ").replace("-", " ")
    # 별칭 매핑
    n = ALIAS_MAP.get(n, n)
    # 여러 공백을 하나로 정규화
    n = " ".join(n.split())
    
    # 디버그 2: _canonicalize 함수의 최종 출력값을 확인
    dprint(f"[DEBUG] _canonicalize output: '{n}'")
    
    return n


def normalize_tech_stacks(
    raw: List[Dict],
    *,
    min_level: int = 1,
    max_level: int = 5,
    allow_unknown: bool = True,
) -> List[Dict]:
    """
    입력 예시: [{"name":"Spring","level":5}, {"name":"Vue.js","level":3}]
    출력 예시: [{"name":"spring","level":5}, {"name":"vue.js","level":3}]

    - 이름 표준화/별칭 매핑
    - level 범위를 [min_level, max_level]로 클리핑
    - 중복 name은 더 높은 level로 병합
    - allow_unknown=False이면 ALLOWED_TECH 외 항목은 제거
    """
    # 디버그 3: normalize_tech_stacks 함수의 원본 입력값 확인
    dprint(f"[DEBUG] normalize_tech_stacks raw input: {raw}")
    
    merged: Dict[str, int] = {}

    for item in (raw or []):
        name = _canonicalize(str(item.get("name", "")))
        if not name:
            continue

        level = item.get("level", (min_level + max_level) // 2)
        try:
            level = int(level)
        except Exception:
            level = (min_level + max_level) // 2
        level = max(min_level, min(max_level, level))

        if not allow_unknown and name not in ALLOWED_TECH:
            continue

        if name in merged:
            merged[name] = max(merged[name], level)
        else:
            merged[name] = level

    # 디버그 4: 정규화 후 병합된 딕셔너리 확인
    dprint(f"[DEBUG] normalize_tech_stacks merged dict: {merged}")
    
    normalized = [{"name": k, "level": v} for k, v in merged.items()]
    normalized.sort(key=lambda x: (-x["level"], x["name"]))

    # 디버그 5: 최종 정규화된 리스트 확인
    dprint(f"[DEBUG] normalize_tech_stacks final output: {normalized}")
    
    return normalized


def to_name_list(normalized: List[Dict]) -> List[str]:
    """모델 피처 입력용으로 name 리스트만 추출"""
    return [x.get("name") for x in (normalized or []) if x.get("name")]


def debug_preview(raw: List[Dict]) -> Tuple[List[Dict], List[str]]:
    n = normalize_tech_stacks(raw)
    return n, to_name_list(n)
