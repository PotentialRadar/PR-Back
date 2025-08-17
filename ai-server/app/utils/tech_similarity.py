# app/utils/tech_similarity.py
"""
기술스택 간 연관성 매트릭스 및 유사도 계산
"""

# 기술스택 연관성 매트릭스 (0.0 ~ 1.0)
TECH_SIMILARITY_MATRIX = {
    # JavaScript 생태계
    "JavaScript": {
        "TypeScript": 0.9,  # 매우 높은 연관성
        "React": 0.8,
        "Vue.js": 0.8,
        "Node.js": 0.9,
        "Express": 0.8,
        "Angular": 0.7,
        "Svelte": 0.7,
        "Nuxt.js": 0.6,
        "React Native": 0.7,
    },
    
    # TypeScript 생태계
    "TypeScript": {
        "JavaScript": 0.9,
        "React": 0.8,
        "Vue.js": 0.7,
        "Node.js": 0.8,
        "Angular": 0.9,  # Angular는 TypeScript 기본
        "Express": 0.7,
        "Svelte": 0.6,
        "React Native": 0.7,
    },
    
    # React 생태계
    "React": {
        "JavaScript": 0.8,
        "TypeScript": 0.8,
        "Node.js": 0.7,
        "Express": 0.6,
        "React Native": 0.9,  # 매우 유사
        "MongoDB": 0.5,
        "Redux": 0.8,
    },
    
    # Vue.js 생태계
    "Vue.js": {
        "JavaScript": 0.8,
        "TypeScript": 0.7,
        "Nuxt.js": 0.9,  # Nuxt는 Vue 기반
        "Tailwind CSS": 0.6,
    },
    
    # Python 생태계
    "Python": {
        "Django": 0.9,
        "FastAPI": 0.8,
        "SQLAlchemy": 0.7,
        "PostgreSQL": 0.6,
        "Redis": 0.5,
        "TensorFlow": 0.8,
        "scikit-learn": 0.8,
        "Pandas": 0.8,
        "NumPy": 0.7,
    },
    
    # Java 생태계
    "Java": {
        "Spring Boot": 0.9,
        "PostgreSQL": 0.6,
        "MySQL": 0.6,
        "Redis": 0.5,
        "Docker": 0.6,
        "Kubernetes": 0.6,
    },
    
    # Node.js 생태계
    "Node.js": {
        "JavaScript": 0.9,
        "TypeScript": 0.8,
        "Express": 0.9,
        "MongoDB": 0.7,
        "Redis": 0.6,
        "Socket.io": 0.7,
    },
    
    # 데이터베이스 연관성
    "PostgreSQL": {
        "Django": 0.7,
        "FastAPI": 0.6,
        "Spring Boot": 0.6,
        "SQLAlchemy": 0.8,
        "Docker": 0.5,
    },
    
    "MongoDB": {
        "Node.js": 0.7,
        "Express": 0.7,
        "React": 0.5,
        "JavaScript": 0.6,
    },
    
    "Redis": {
        "Python": 0.5,
        "Node.js": 0.6,
        "Java": 0.5,
        "Django": 0.6,
        "Express": 0.5,
    },
    
    # DevOps 연관성
    "Docker": {
        "Kubernetes": 0.8,
        "Spring Boot": 0.6,
        "Django": 0.5,
        "PostgreSQL": 0.5,
        "Go": 0.6,
        "Rust": 0.5,
    },
    
    "Kubernetes": {
        "Docker": 0.8,
        "Terraform": 0.7,
        "AWS": 0.6,
        "Jenkins": 0.5,
    },
    
    # 모바일 연관성
    "React Native": {
        "React": 0.9,
        "JavaScript": 0.7,
        "TypeScript": 0.7,
        "Firebase": 0.6,
        "Expo": 0.8,
    },
    
    "Flutter": {
        "Dart": 0.9,
        "Firebase": 0.7,
        "SQLite": 0.5,
    },
    
    # 기타 프레임워크
    "Spring Boot": {
        "Java": 0.9,
        "PostgreSQL": 0.6,
        "MySQL": 0.6,
        "Docker": 0.6,
        "Kubernetes": 0.6,
    },
    
    "Django": {
        "Python": 0.9,
        "PostgreSQL": 0.7,
        "Redis": 0.6,
        "Docker": 0.5,
    },
    
    "FastAPI": {
        "Python": 0.8,
        "SQLAlchemy": 0.7,
        "PostgreSQL": 0.6,
        "Redis": 0.5,
    },
}

def get_tech_similarity(tech1: str, tech2: str) -> float:
    """
    두 기술스택 간의 유사도를 반환합니다.
    
    Args:
        tech1: 첫 번째 기술스택
        tech2: 두 번째 기술스택
    
    Returns:
        0.0 ~ 1.0 사이의 유사도 점수
    """
    if tech1 == tech2:
        return 1.0
    
    # tech1에서 tech2 찾기
    if tech1 in TECH_SIMILARITY_MATRIX:
        if tech2 in TECH_SIMILARITY_MATRIX[tech1]:
            return TECH_SIMILARITY_MATRIX[tech1][tech2]
    
    # tech2에서 tech1 찾기 (대칭성)
    if tech2 in TECH_SIMILARITY_MATRIX:
        if tech1 in TECH_SIMILARITY_MATRIX[tech2]:
            return TECH_SIMILARITY_MATRIX[tech2][tech1]
    
    # 연관성이 정의되지 않은 경우 기본값
    return 0.1

def enhanced_similarity_score(user_techs: list, project_techs: list) -> float:
    """
    기술스택 연관성을 고려한 향상된 유사도 점수 계산
    
    Args:
        user_techs: 사용자 기술스택 리스트
        project_techs: 프로젝트 기술스택 리스트
    
    Returns:
        향상된 유사도 점수 (0.0 ~ 1.0)
    """
    if not user_techs or not project_techs:
        return 0.0
    
    total_score = 0.0
    max_possible_score = 0.0
    
    for user_tech in user_techs:
        # 각 사용자 기술에 대해 프로젝트 기술들과의 최대 유사도를 찾음
        best_match_score = 0.0
        for project_tech in project_techs:
            similarity = get_tech_similarity(user_tech, project_tech)
            best_match_score = max(best_match_score, similarity)
        
        total_score += best_match_score
        max_possible_score += 1.0  # 완벽한 매치라면 1.0
    
    # 정규화: 사용자 기술스택 수로 나누어 평균 유사도 계산
    return total_score / len(user_techs) if user_techs else 0.0

def symmetric_enhanced_similarity(user_techs: list, project_techs: list) -> float:
    """
    양방향 향상된 유사도 (사용자 → 프로젝트, 프로젝트 → 사용자)
    """
    # 사용자 기술스택 기준에서 프로젝트와의 유사도
    user_to_project = enhanced_similarity_score(user_techs, project_techs)
    
    # 프로젝트 기술스택 기준에서 사용자와의 유사도
    project_to_user = enhanced_similarity_score(project_techs, user_techs)
    
    # 두 방향의 평균 (대칭성 보장)
    return (user_to_project + project_to_user) / 2.0