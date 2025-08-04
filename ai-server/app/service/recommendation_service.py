from typing import List
from ..schemas import UserTechStack, ProjectRecommendation

# def get_recommended_projects(user: UserTechStack) -> List[ProjectRecommendation]:
#     # 1. 요청으로 들어온 사용자 정보를 출력
#     print(f"사용자 ID: {user.user_id}")
#     print(f"사용자 기술 스택: {user.tech_stacks}") # 예시, 실제 UserTechStack 모델에 맞게 수정

#     # 2. 여기에 추천 로직이 들어갑니다.
#     # 추천 로직이 올바르게 동작하는지 확인하기 위해 단계별로 데이터를 출력해보세요.

#     # 3. 반환할 프로젝트 목록을 출력
#     recommended_projects = [] # 로직의 결과
#     print(f"추천 로직 결과: {recommended_projects}")

#     return recommended_projects


def get_recommended_projects(user: UserTechStack) -> List[ProjectRecommendation]:
    # 예시: Spring 사용자를 위한 더미 프로젝트 추천
    stack_names = [stack.name for stack in user.tech_stacks]

    dummy_projects = [
    ProjectRecommendation(
        projectId=1,
        title="Spring Boot 프로젝트",
        description="Spring 관련 예제",
        matchScore=0.95,
        projectTechStacks=["Spring", "Spring Boot"]
    ),
    ProjectRecommendation(
        projectId=2,
        title="Vue.js 프로젝트",
        description="Vue.js 관련 예제",
        matchScore=0.87,
        projectTechStacks=["Vue.js"]
    )
]

    recommended_projects = [
        project for project in dummy_projects if any(name in project.title for name in stack_names)
    ]

    return recommended_projects