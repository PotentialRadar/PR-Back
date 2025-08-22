"""
Pydantic 스키마 정의
"""
from pydantic import BaseModel, Field, validator
from typing import List, Optional, Dict
from datetime import datetime

class UserTechStack(BaseModel):
    """사용자 기술스택 정보"""
    name: str = Field(..., description="기술스택 이름", min_length=1, max_length=50)
    level: int = Field(..., description="기술 수준 (1-5)", ge=1, le=5)
    
    @validator('name')
    def validate_name(cls, v):
        if not v or not v.strip():
            raise ValueError('기술스택 이름은 비어있을 수 없습니다')
        return v.strip().lower()

class LikedProject(BaseModel):
    """사용자가 좋아요한 프로젝트 정보"""
    projectId: int = Field(..., description="프로젝트 ID", gt=0)
    title: str = Field(..., description="프로젝트 제목", max_length=200)
    techStacks: List[str] = Field(default_factory=list, description="프로젝트 기술스택")
    likedAt: datetime = Field(..., description="좋아요 시점")
    category: str = Field(default="기타", description="프로젝트 카테고리")

class RecommendRequest(BaseModel):
    """프로젝트 추천 요청 스키마"""
    user_id: int = Field(..., alias="userId", description="사용자 ID", gt=0)
    tech_stacks: List[UserTechStack] = Field(
        ..., 
        alias="techStacks", 
        description="사용자 기술스택 리스트",
        min_items=1,
        max_items=20
    )
    liked_projects: List[LikedProject] = Field(
        default_factory=list,
        alias="likedProjects",
        description="사용자가 좋아요한 프로젝트 리스트",
        max_items=50
    )
    include_likes: bool = Field(
        default=True,
        alias="includeLikes", 
        description="좋아요 데이터 포함 여부"
    )

    class Config:
        allow_population_by_field_name = True
        schema_extra = {
            "example": {
                "userId": 1,
                "techStacks": [
                    {"name": "spring", "level": 4},
                    {"name": "vue.js", "level": 3}
                ]
            }
        }

class RecommendationExplanation(BaseModel):
    """추천 설명 스키마"""
    main_reason: str = Field(..., description="주요 추천 이유")
    detailed_reasons: List[str] = Field(default_factory=list, description="상세 이유 목록")
    score_breakdown: Dict[str, float] = Field(default_factory=dict, description="점수 분해")
    matched_skills: List[str] = Field(default_factory=list, description="일치하는 기술")
    growth_opportunities: List[str] = Field(default_factory=list, description="성장 기회")
    simple_explanation: Optional[str] = Field(None, description="간단한 설명")

class ProjectRecommendation(BaseModel):
    """프로젝트 추천 결과 스키마"""
    projectId: int = Field(..., description="프로젝트 ID")
    title: str = Field(..., description="프로젝트 제목", max_length=200)
    description: str = Field(..., description="프로젝트 설명", max_length=1000)
    matchScore: float = Field(
        ..., 
        description="매칭 점수 (0.0-1.0)", 
        ge=0.0, 
        le=1.0
    )
    projectTechStacks: List[str] = Field(
        default_factory=list, 
        description="프로젝트 기술스택 리스트"
    )
    explanation: Optional[RecommendationExplanation] = Field(
        None, 
        description="추천 설명 정보"
    )
    
    class Config:
        schema_extra = {
            "example": {
                "projectId": 1,
                "title": "AI 추천 시스템",
                "description": "FastAPI와 Vue.js를 사용한 AI 추천 시스템 개발",
                "matchScore": 0.85,
                "projectTechStacks": ["fastapi", "vue.js", "postgresql"]
            }
        }


class HealthResponse(BaseModel):
    """헬스 체크 응답 스키마"""
    status: str = Field(..., description="서버 상태")
    ml_model_loaded: bool = Field(..., description="ML 모델 로드 상태")
    version: str = Field(..., description="서버 버전")

# 팀원 추천 관련 스키마
class MemberExplanation(BaseModel):
    main_reason: str = Field(..., description="주요 추천 이유")
    detailed_reasons: List[str] = Field(default_factory=list, description="상세 이유 목록")
    matched_skills: List[str] = Field(default_factory=list, description="매칭된 기술스택")
    growth_opportunities: List[str] = Field(default_factory=list, description="성장 가능한 기술")
    simple_explanation: str = Field(..., description="간단한 한 줄 설명")
    experience_match: str = Field(..., description="경험 수준 매칭 설명")

class RecommendMemberRequest(BaseModel):
    projectId: int = Field(..., description="프로젝트 ID")
    requiredSkills: List[str] = Field(..., description="필요한 기술스택 목록")
    teamSize: int = Field(default=4, description="추천받을 팀원 수")
    experienceLevel: str = Field(default="any", description="경험 수준")

class RecommendedMember(BaseModel):
    userId: int = Field(..., description="사용자 ID")
    name: str = Field(..., description="사용자 이름")
    email: Optional[str] = Field(None, description="이메일")
    profileImage: Optional[str] = Field(None, description="프로필 이미지")
    matchScore: float = Field(..., ge=0, le=1, description="매칭 점수 (0-1)")
    userTechStacks: List[UserTechStack] = Field(default_factory=list, description="사용자 기술스택")
    explanation: MemberExplanation = Field(..., description="추천 설명")
    experience: str = Field(..., description="경력")
    portfolioCount: int = Field(default=0, description="포트폴리오 프로젝트 수")
    completedProjects: int = Field(default=0, description="완료한 프로젝트 수")
    averageRating: Optional[float] = Field(None, description="평균 평점")
    lastActiveDate: str = Field(..., description="마지막 활동일")
    isAvailable: bool = Field(default=True, description="현재 참여 가능 여부")
    currentProjectCount: int = Field(default=0, description="현재 참여 중인 프로젝트 수")

# 프로젝트 추천 관련 스키마 (새로 추가)
class RecommendProjectRequest(BaseModel):
    userId: int
    techStacks: List[UserTechStack]
    experienceLevel: Optional[str] = "intermediate"
    preferredCategories: Optional[List[str]] = []
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
    explanation: Optional[ProjectExplanation]