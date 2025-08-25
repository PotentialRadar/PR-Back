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
    # 개발 도구들
    "vscode": "visual studio code",
    "vs code": "visual studio code",
    "intellij": "intellij idea",
    "pycharm": "intellij idea",
    "webstorm": "intellij idea",
    "netbeans": "netbeans ide",
}

# 데이터베이스에서 동적으로 가져올 기술스택 캐시
_ALLOWED_TECH_CACHE = None

def get_allowed_tech_from_db():
    """데이터베이스에서 실제 기술스택들을 가져옵니다."""
    global _ALLOWED_TECH_CACHE
    
    if _ALLOWED_TECH_CACHE is not None:
        return _ALLOWED_TECH_CACHE
    
    try:
        from app.database import get_db
        from app.models import TechStack
        
        # DB 연결해서 실제 기술스택들 가져오기
        db = next(get_db())
        tech_stacks = db.query(TechStack).all()
        
        # 소문자로 정규화해서 set 생성
        _ALLOWED_TECH_CACHE = {stack.name.lower() for stack in tech_stacks}
        
        dprint(f"[DEBUG] Loaded {len(_ALLOWED_TECH_CACHE)} tech stacks from database")
        return _ALLOWED_TECH_CACHE
        
    except Exception as e:
        dprint(f"[DEBUG] Failed to load tech stacks from DB: {e}")
        # DB 연결 실패시 fallback으로 기본 세트 사용
        return get_fallback_allowed_tech()

def get_fallback_allowed_tech():
    """DB 연결 실패시 사용할 기본 기술스택들"""
    return {
        # 핵심 기술들만
        "react", "vue.js", "angular", "next.js", "typescript", "javascript",
        "spring boot", "node.js", "express.js", "django", "fastapi", "java", "python", "c#",
        "postgresql", "mongodb", "redis", "mysql", "docker", "kubernetes", "aws",
        "react native", "flutter", "unity 3d", "tensorflow", "solidity", "web3.js"
    }

# 기존 하드코딩된 ALLOWED_TECH는 제거하고 함수로 대체
def get_allowed_tech():
    """허용된 기술스택 세트를 반환합니다. DB에서 동적으로 가져옵니다."""
    return get_allowed_tech_from_db()

# 개발도구 -> 실제 개발 기술스택 확장 매핑
DEV_TOOL_EXPANSIONS = {
    "visual studio code": ["javascript", "typescript", "python", "react", "node.js"],
    "intellij idea": ["java", "kotlin", "spring boot", "scala"],
    "pycharm": ["python", "django", "fastapi", "tensorflow"],
    "webstorm": ["javascript", "typescript", "react", "vue.js", "node.js"],
    "android studio": ["kotlin", "java", "android sdk", "flutter"],
    "xcode": ["swift", "objective-c", "ios", "swiftui"],
    "github actions": ["docker", "kubernetes", "ci/cd", "devops"],
    "qlikview": ["sql", "data analysis", "business intelligence"],
    "cisco": ["networking", "tcp/ip", "network security"],
    "vulkan": ["c++", "graphics programming", "game development"]
}

def expand_dev_tools_to_tech_stacks(tech_names: List[str]) -> List[str]:
    """개발도구를 실제 기술스택들로 확장합니다."""
    expanded = set(tech_names)  # 기존 기술스택들은 유지
    
    for tech in tech_names:
        if tech in DEV_TOOL_EXPANSIONS:
            expanded.update(DEV_TOOL_EXPANSIONS[tech])
            dprint(f"[DEBUG] 개발도구 '{tech}' -> {DEV_TOOL_EXPANSIONS[tech]}로 확장")
    
    return list(expanded)


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

        if not allow_unknown and name not in get_allowed_tech():
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
