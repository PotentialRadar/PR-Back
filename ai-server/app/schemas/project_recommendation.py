from pydantic import BaseModel
from typing import List, Optional

class UserTechStack(BaseModel):
    name: str
    level: int  # 1-5

class RecommendProjectRequest(BaseModel):
    userId: int
    techStacks: List[UserTechStack]
    experienceLevel: Optional[str] = "intermediate"  # beginner, intermediate, advanced
    preferredCategories: Optional[List[str]] = []    # ["frontend", "backend", "fullstack"]
    maxResults: Optional[int] = 5

class ProjectExplanation(BaseModel):
    main_reason: str
    matched_skills: List[str]
    growth_opportunities: List[str]
    simple_explanation: str
    difficulty_level: str
    learning_potential: float

class RecommendedProject(BaseModel):
    projectId: int
    title: str
    description: str
    matchScore: float
    projectTechStacks: List[str]
    status: str
    recruitDeadline: str
    startDate: str
    endDate: str
    recruitCount: int
    appliedCount: int
    viewCount: int
    explanation: ProjectExplanation