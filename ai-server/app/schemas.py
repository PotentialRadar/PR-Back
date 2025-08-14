from pydantic import BaseModel, Field
from typing import List

class UserTechStack(BaseModel):
    name: str
    level: int

class RecommendRequest(BaseModel):
    user_id: int = Field(..., alias="userId")
    tech_stacks: List[UserTechStack] = Field(..., alias="techStacks")

    class Config:
        allow_population_by_field_name = True  # 선택적: dict 변환 시에도 snake_case 유지

class ProjectRecommendation(BaseModel):
    projectId: int
    title: str
    description: str
    matchScore: float
    projectTechStacks: List[str] = []  # 혹은 Optional[List[str]] = None