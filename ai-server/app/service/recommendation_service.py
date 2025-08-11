# app/service/recommendation_service.py

from typing import List
from ..schemas import UserTechStack, ProjectRecommendation, RecommendRequest
import joblib

# 더미 프로젝트 데이터
# 실제로는 데이터베이스에서 가져와야 합니다.
PROJECT_DATA = [
    {
        "id": 1,
        "title": "A Modern Web Service with Spring Boot",
        "description": "Building a RESTful API using Spring Boot and JPA.",
        "tech_stacks": ["spring boot", "spring data jpa", "docker", "postgreSQL"]
    },
    {
        "id": 2,
        "title": "E-commerce Front-end with Vue.js",
        "description": "Developing a dynamic front-end using Vue.js and Pinia.",
        "tech_stacks": ["vue.js", "pinia", "javascript", "html", "css"]
    },
    {
        "id": 3,
        "title": "Legacy Spring MVC Application Migration",
        "description": "Migrating an old Spring MVC app to a modern microservice architecture.",
        "tech_stacks": ["spring mvc", "javascript", "jquery", "oracle db"]
    },
    {
        "id": 4,
        "title": "Simple Website with React",
        "description": "A basic portfolio website project using React.",
        "tech_stacks": ["react", "html", "css"]
    }
]

# 기술 스택 간의 관계를 정의합니다.
# key에 해당하는 기술을 가지고 있으면, value에 해당하는 기술 스택과도 연관이 있다고 판단합니다.
# 이 딕셔너리에 추가적인 기술 관계를 정의할 수 있습니다.
TECH_RELATIONS = {
    "spring": {"spring boot", "spring mvc", "spring security", "spring data jpa"},
    "vue.js": {"vue.js"},
    "react": {"react"},
    "javascript": {"vue.js", "react", "typescript"}
}

# 모델 로딩: 실제 경로에 맞게 수정하세요.
try:
    model = joblib.load("app/model/recommender.pkl")
except FileNotFoundError:
    print("Warning: recommender.pkl not found. Using a dummy model.")
    # 모델 파일이 없을 경우를 대비하여 더미 모델을 사용합니다.
    # 이 더미 모델은 tech_overlap 값에 비례하여 점수를 반환합니다.
    class DummyModel:
        def predict_proba(self, features):
            tech_overlap = features[0][2]
            return [[1 - tech_overlap, tech_overlap]]
    model = DummyModel()

def calculate_overlap_score(user_stacks: List[str], project_stacks: List[str]) -> float:
    """
    사용자 기술 스택과 프로젝트 기술 스택의 겹치는 정도를 계산합니다.
    단순 일치뿐만 아니라, 관련 기술 스택에 대해서도 부분 점수를 부여합니다.
    """
    if not user_stacks or not project_stacks:
        return 0.0

    normalized_user_stacks = {stack.lower() for stack in user_stacks}
    normalized_project_stacks = {stack.lower() for stack in project_stacks}
    
    total_score = 0.0
    
    for user_stack in normalized_user_stacks:
        # 정확히 일치하는 경우 (가중치 1.0)
        if user_stack in normalized_project_stacks:
            total_score += 1.0
        # 관련 기술이 프로젝트에 있는 경우 (가중치 0.5)
        else:
            related_techs = TECH_RELATIONS.get(user_stack, set())
            if not related_techs.isdisjoint(normalized_project_stacks):
                total_score += 0.5

    # 겹치는 점수를 프로젝트 스택 개수로 나누어 정규화합니다.
    if len(normalized_project_stacks) == 0:
        return 0.0
    
    return total_score / len(normalized_project_stacks)

def get_recommended_projects(request: RecommendRequest) -> List[ProjectRecommendation]:
    user_tech_stacks = [stack.name for stack in request.tech_stacks]
    user_id = request.userId

    recommended_projects = []

    for project in PROJECT_DATA:
        project_id = project['id']
        project_tech_stacks = project['tech_stacks']
        
        # 개선된 함수를 사용하여 tech_overlap 계산
        tech_overlap = calculate_overlap_score(user_tech_stacks, project_tech_stacks)
        
        # 디버그 출력 추가: tech_overlap이 어떻게 계산되는지 직접 확인
        print(f"[DEBUG] Processing Project ID: {project_id}")
        print(f"[DEBUG]   User Stacks: {user_tech_stacks}")
        print(f"[DEBUG]   Project Stacks: {project_tech_stacks}")
        print(f"[DEBUG]   Calculated tech_overlap: {tech_overlap}")
        
        # 모델을 사용하여 최종 점수 예측
        # 더미 모델의 경우 tech_overlap과 동일한 값이 반환될 것으로 예상됩니다.
        score = model.predict_proba([[user_id, project_id, tech_overlap]])[0][1]
        
        print(f"[DEBUG]   Final Score: {score}\n")
        
        recommended_projects.append(
            ProjectRecommendation(
                projectId=project_id,
                title=project['title'],
                description=project['description'],
                matchScore=score,
                projectTechStacks=project_tech_stacks
            )
        )
    
    recommended_projects.sort(key=lambda x: x.matchScore, reverse=True)
    return recommended_projects
