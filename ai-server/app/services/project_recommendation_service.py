import random
from typing import List, Dict, Any
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sqlalchemy.orm import Session
from app.schemas import RecommendProjectRequest, RecommendedProject, ProjectExplanation
from app.models import Project, ProjectTechStack

class ProjectRecommendationService:
    def __init__(self):
        # 기술 스택별 카테고리 매핑
        self.tech_categories = {
            "React": "frontend",
            "Vue.js": "frontend", 
            "Angular": "frontend",
            "TypeScript": "frontend",
            "JavaScript": "frontend",
            "HTML": "frontend",
            "CSS": "frontend",
            "Svelte": "frontend",
            "Next.js": "frontend",
            "Nuxt.js": "frontend",
            
            "Java": "backend",
            "Spring Boot": "backend",
            "Python": "backend", 
            "Django": "backend",
            "FastAPI": "backend",
            "Node.js": "fullstack",
            "Express": "backend",
            "Go": "backend",
            "Rust": "backend",
            "C#": "backend",
            ".NET Core": "backend",
            "PHP": "backend",
            "Laravel": "backend",
            
            "React Native": "mobile",
            "Flutter": "mobile",
            "Swift": "mobile",
            "Kotlin": "mobile",
            "Android": "mobile",
            "iOS": "mobile",
            
            "Docker": "infra",
            "Kubernetes": "infra", 
            "AWS": "infra",
            "Azure": "infra",
            "GCP": "infra",
            "Terraform": "infra",
            "Jenkins": "infra",
            
            "PostgreSQL": "database",
            "MySQL": "database",
            "MongoDB": "database",
            "Redis": "database",
            "SQLite": "database",
            
            "TensorFlow": "ai",
            "PyTorch": "ai",
            "scikit-learn": "ai",
            "NLP": "ai",
            "OpenCV": "ai"
        }
        
        # 기술 스택 간 연관성 매트릭스 (0.0-1.0)
        self.tech_similarity = {
            ("React", "TypeScript"): 0.9,
            ("React", "JavaScript"): 0.9,
            ("Vue.js", "TypeScript"): 0.8,
            ("Vue.js", "JavaScript"): 0.9,
            ("Angular", "TypeScript"): 0.95,
            ("React", "Node.js"): 0.8,
            ("Vue.js", "Node.js"): 0.7,
            ("Java", "Spring Boot"): 0.95,
            ("Python", "Django"): 0.9,
            ("Python", "FastAPI"): 0.85,
            ("JavaScript", "Node.js"): 0.9,
            ("TypeScript", "Node.js"): 0.85,
            ("Docker", "Kubernetes"): 0.9,
            ("AWS", "Docker"): 0.8,
            ("PostgreSQL", "Django"): 0.8,
            ("MongoDB", "Node.js"): 0.8,
        }
        
        # Mock 프로젝트 데이터
        self.mock_projects = [
            {
                "projectId": 1,
                "title": "React 기반 쇼핑몰 개발",
                "description": "TypeScript와 React를 활용한 현대적인 이커머스 플랫폼 개발",
                "techStacks": ["React", "TypeScript", "Node.js", "Express", "MongoDB"],
                "category": "frontend",
                "difficulty": "intermediate",
                "recruitCount": 3,
                "appliedCount": 8,
                "viewCount": 142,
                "recruitDeadline": "2025-08-25",
                "startDate": "2025-09-01",
                "endDate": "2025-12-15",
                "status": "RECRUITING"
            },
            {
                "projectId": 2,
                "title": "Vue3 + Nuxt 포트폴리오 사이트", 
                "description": "Nuxt3와 Vue3 Composition API를 활용한 개인 포트폴리오 웹사이트",
                "techStacks": ["Vue.js", "Nuxt.js", "TypeScript", "Tailwind CSS"],
                "category": "frontend",
                "difficulty": "beginner",
                "recruitCount": 2,
                "appliedCount": 12,
                "viewCount": 89,
                "recruitDeadline": "2025-08-30",
                "startDate": "2025-09-10", 
                "endDate": "2025-11-30",
                "status": "RECRUITING"
            },
            {
                "projectId": 3,
                "title": "Django REST API 서버 구축",
                "description": "Python Django를 활용한 RESTful API 서버와 관리자 페이지 개발",
                "techStacks": ["Python", "Django", "PostgreSQL", "Redis", "Docker"],
                "category": "backend", 
                "difficulty": "intermediate",
                "recruitCount": 4,
                "appliedCount": 5,
                "viewCount": 93,
                "recruitDeadline": "2025-09-01",
                "startDate": "2025-09-15",
                "endDate": "2025-12-01",
                "status": "RECRUITING"
            },
            {
                "projectId": 4,
                "title": "Spring Boot 마이크로서비스",
                "description": "MSA 아키텍처 기반 Spring Boot 서비스 개발 및 Docker 컨테이너화", 
                "techStacks": ["Java", "Spring Boot", "Docker", "Kubernetes", "PostgreSQL"],
                "category": "backend",
                "difficulty": "advanced",
                "recruitCount": 5,
                "appliedCount": 14,
                "viewCount": 125,
                "recruitDeadline": "2025-08-22",
                "startDate": "2025-09-05",
                "endDate": "2026-01-15",
                "status": "RECRUITING"
            },
            {
                "projectId": 5,
                "title": "React Native 투두 앱",
                "description": "TypeScript와 React Native를 활용한 크로스플랫폼 모바일 앱",
                "techStacks": ["React Native", "TypeScript", "Expo", "Firebase"],
                "category": "mobile",
                "difficulty": "intermediate", 
                "recruitCount": 2,
                "appliedCount": 7,
                "viewCount": 98,
                "recruitDeadline": "2025-08-30",
                "startDate": "2025-09-15",
                "endDate": "2025-11-25",
                "status": "RECRUITING"
            },
            {
                "projectId": 6,
                "title": "Flutter 전자상거래 앱",
                "description": "Dart와 Flutter를 활용한 모바일 쇼핑 애플리케이션",
                "techStacks": ["Flutter", "Dart", "Firebase", "SQLite"],
                "category": "mobile",
                "difficulty": "intermediate",
                "recruitCount": 4,
                "appliedCount": 12,
                "viewCount": 107,
                "recruitDeadline": "2025-09-01", 
                "startDate": "2025-09-20",
                "endDate": "2025-12-30",
                "status": "RECRUITING"
            },
            {
                "projectId": 7,
                "title": "MSA 기반 클라우드 인프라 구축",
                "description": "마이크로서비스 아키텍처와 컨테이너 기반 인프라 구축",
                "techStacks": ["Docker", "Kubernetes", "AWS", "MSA", "Jenkins"],
                "category": "infra",
                "difficulty": "advanced",
                "recruitCount": 5,
                "appliedCount": 20,
                "viewCount": 321,
                "recruitDeadline": "2025-09-15",
                "startDate": "2025-10-01", 
                "endDate": "2025-12-10",
                "status": "RECRUITING"
            },
            {
                "projectId": 8,
                "title": "FastAPI 비동기 웹서버",
                "description": "Python FastAPI를 활용한 고성능 비동기 웹 서버 및 API 개발",
                "techStacks": ["Python", "FastAPI", "SQLAlchemy", "PostgreSQL", "Redis"],
                "category": "backend",
                "difficulty": "intermediate",
                "recruitCount": 3,
                "appliedCount": 9,
                "viewCount": 141,
                "recruitDeadline": "2025-09-05",
                "startDate": "2025-09-25",
                "endDate": "2026-01-10", 
                "status": "RECRUITING"
            },
            {
                "projectId": 9,
                "title": "Python ML 추천시스템",
                "description": "TensorFlow와 scikit-learn을 활용한 AI 기반 상품 추천 시스템",
                "techStacks": ["Python", "TensorFlow", "scikit-learn", "Pandas", "NumPy", "FastAPI"],
                "category": "ai",
                "difficulty": "advanced",
                "recruitCount": 4,
                "appliedCount": 16,
                "viewCount": 169,
                "recruitDeadline": "2025-10-05",
                "startDate": "2025-11-15",
                "endDate": "2026-04-01",
                "status": "RECRUITING"
            },
            {
                "projectId": 10,
                "title": "Angular 관리자 대시보드",
                "description": "Angular와 TypeScript로 구축하는 기업용 관리자 패널",
                "techStacks": ["Angular", "TypeScript", "RxJS", "Material UI", "Chart.js"],
                "category": "frontend",
                "difficulty": "intermediate",
                "recruitCount": 5,
                "appliedCount": 11,
                "viewCount": 78,
                "recruitDeadline": "2025-09-10",
                "startDate": "2025-10-01",
                "endDate": "2026-02-28",
                "status": "RECRUITING"
            }
        ]
    
    def get_projects_from_db(self, db: Session) -> List[Dict]:
        """DB에서 실제 프로젝트 데이터를 조회"""
        try:
            projects = db.query(Project).all()
            db_projects = []
            
            for project in projects:
                # 프로젝트의 기술스택 조회 (관계를 통해 tech_stack.tech_stack_name 가져오기)
                tech_stacks = [ts.tech_stack.tech_stack_name for ts in project.tech_stacks]
                
                # Mock 데이터 형식에 맞게 변환
                project_data = {
                    "projectId": project.project_id,
                    "title": project.title,
                    "description": project.description or "",
                    "techStacks": tech_stacks,
                    "category": self._determine_category(tech_stacks),
                    "difficulty": "intermediate",  # 기본값, 추후 DB 컬럼 추가 가능
                    "recruitCount": 3,  # 기본값, 추후 DB 컬럼 추가 가능
                    "appliedCount": random.randint(1, 10),
                    "viewCount": random.randint(50, 200),
                    "recruitDeadline": "2025-12-31",  # 기본값
                    "startDate": "2025-09-01",  # 기본값
                    "endDate": "2025-12-31",  # 기본값
                    "status": "RECRUITING"  # 기본값
                }
                db_projects.append(project_data)
            
            return db_projects
        except Exception as e:
            print(f"DB 조회 실패: {e}")
            return self.mock_projects  # 실패시 Mock 데이터 사용
    
    def _determine_category(self, tech_stacks: List[str]) -> str:
        """기술스택 기반으로 프로젝트 카테고리 결정"""
        if not tech_stacks:
            return "unknown"
        
        # 기술스택별 카테고리 점수 계산
        category_scores = {}
        for tech in tech_stacks:
            category = self.tech_categories.get(tech, "unknown")
            category_scores[category] = category_scores.get(category, 0) + 1
        
        # 가장 많이 등장한 카테고리 반환
        return max(category_scores, key=category_scores.get) if category_scores else "unknown"

    def calculate_tech_match_score(self, user_techs: List[Dict], project_techs: List[str]) -> float:
        """사용자 기술스택과 프로젝트 기술스택 간의 매칭 점수 계산"""
        if not user_techs or not project_techs:
            return 0.0
            
        user_tech_names = [tech["name"] for tech in user_techs]
        user_tech_levels = {tech["name"]: tech["level"] for tech in user_techs}
        
        # 직접 매칭
        direct_matches = set(user_tech_names) & set(project_techs)
        direct_score = len(direct_matches) / len(project_techs) if project_techs else 0
        
        # 연관 기술 매칭
        similarity_score = 0.0
        similarity_count = 0
        
        for user_tech in user_tech_names:
            for project_tech in project_techs:
                # 양방향 유사도 체크
                key1 = (user_tech, project_tech)
                key2 = (project_tech, user_tech)
                
                similarity = self.tech_similarity.get(key1, self.tech_similarity.get(key2, 0))
                if similarity > 0:
                    # 사용자 레벨을 고려한 가중치 적용
                    level_weight = user_tech_levels.get(user_tech, 3) / 5.0
                    similarity_score += similarity * level_weight
                    similarity_count += 1
        
        if similarity_count > 0:
            similarity_score /= similarity_count
            
        # 최종 점수: 직접 매칭 70%, 유사도 30%
        final_score = direct_score * 0.7 + similarity_score * 0.3
        
        return min(final_score, 1.0)

    def calculate_category_bonus(self, user_techs: List[Dict], project_category: str, 
                               preferred_categories: List[str]) -> float:
        """카테고리 선호도 보너스 계산"""
        bonus = 0.0
        
        # 사용자 선호 카테고리 매칭
        if preferred_categories and project_category in preferred_categories:
            bonus += 0.2
            
        # 사용자 기술스택과 프로젝트 카테고리 일치도
        user_categories = [self.tech_categories.get(tech["name"], "unknown") for tech in user_techs]
        category_match_ratio = user_categories.count(project_category) / len(user_categories) if user_categories else 0
        bonus += category_match_ratio * 0.1
        
        return min(bonus, 0.3)

    def calculate_difficulty_match(self, user_techs: List[Dict], project_difficulty: str, 
                                 experience_level: str) -> float:
        """난이도 매칭 점수 계산"""
        # 사용자 평균 스킬 레벨 계산
        avg_level = sum(tech["level"] for tech in user_techs) / len(user_techs) if user_techs else 3
        
        # 경험 레벨을 숫자로 변환
        exp_mapping = {"beginner": 2, "intermediate": 3, "advanced": 4}
        user_exp_level = exp_mapping.get(experience_level, 3)
        
        # 프로젝트 난이도를 숫자로 변환
        difficulty_mapping = {"beginner": 2, "intermediate": 3, "advanced": 4}
        project_level = difficulty_mapping.get(project_difficulty, 3)
        
        # 적정 난이도 계산 (너무 쉽거나 어려우면 감점)
        level_diff = abs((avg_level + user_exp_level) / 2 - project_level)
        
        if level_diff <= 0.5:
            return 0.1  # 완벽한 난이도 매칭
        elif level_diff <= 1.0:
            return 0.05  # 적당한 난이도
        else:
            return -0.05  # 난이도 불일치

    def generate_explanation(self, user_techs: List[Dict], project: Dict, 
                           match_score: float) -> ProjectExplanation:
        """AI 추천 설명 생성"""
        user_tech_names = [tech["name"] for tech in user_techs]
        project_techs = project["techStacks"]
        
        # 매칭된 기술 찾기
        matched_skills = list(set(user_tech_names) & set(project_techs))
        
        # 새로 배울 기술들
        growth_opportunities = [tech for tech in project_techs if tech not in user_tech_names][:3]
        
        # 메인 설명 생성
        if match_score >= 0.8:
            main_reason = f"당신의 {', '.join(matched_skills[:2])} 경험이 이 프로젝트의 핵심 요구사항과 완벽하게 일치합니다"
            simple_explanation = f"기존 기술을 최대한 활용하면서 {', '.join(growth_opportunities[:2])} 등 새로운 기술도 배울 수 있는 이상적인 프로젝트예요"
        elif match_score >= 0.6:
            main_reason = f"{', '.join(matched_skills[:2])} 경험을 바탕으로 새로운 기술 영역에 도전할 수 있는 기회입니다"
            simple_explanation = f"기존 지식을 활용해 {', '.join(growth_opportunities[:2])} 등을 배우며 기술 역량을 확장할 수 있어요"
        else:
            main_reason = f"새로운 분야에 도전하면서 {', '.join(growth_opportunities[:2])} 등 다양한 기술을 학습할 수 있습니다"
            simple_explanation = f"새로운 기술 영역에 도전해 개발 스택을 다양화하고 성장할 기회를 제공하는 프로젝트예요"
        
        # 학습 잠재력 계산
        learning_potential = min(len(growth_opportunities) / len(project_techs) + 0.3, 1.0)
        
        return ProjectExplanation(
            main_reason=main_reason,
            matched_skills=matched_skills,
            growth_opportunities=growth_opportunities,
            simple_explanation=simple_explanation,
            difficulty_level=project["difficulty"],
            learning_potential=learning_potential
        )

    def recommend_projects(self, request: RecommendProjectRequest, 
                         top_n: int = 5, min_score: float = 0.3, db: Session = None) -> List[RecommendedProject]:
        """프로젝트 추천 메인 로직"""
        
        user_techs = [{"name": tech.name, "level": tech.level} for tech in request.techStacks]
        recommendations = []
        
        # DB가 제공되면 실제 데이터 사용, 아니면 Mock 데이터 사용
        projects = self.get_projects_from_db(db) if db else self.mock_projects
        
        for project in projects:
            # 기술 매칭 점수
            tech_score = self.calculate_tech_match_score(user_techs, project["techStacks"])
            
            # 카테고리 보너스
            category_bonus = self.calculate_category_bonus(
                user_techs, project["category"], request.preferredCategories or []
            )
            
            # 난이도 매칭
            difficulty_bonus = self.calculate_difficulty_match(
                user_techs, project["difficulty"], request.experienceLevel or "intermediate"
            )
            
            # 최종 점수 계산
            final_score = tech_score + category_bonus + difficulty_bonus
            final_score = max(0.0, min(1.0, final_score))  # 0-1 범위로 정규화
            
            if final_score >= min_score:
                # 설명 생성
                explanation = self.generate_explanation(user_techs, project, final_score)
                
                recommendation = RecommendedProject(
                    projectId=project["projectId"],
                    title=project["title"],
                    description=project["description"],
                    matchScore=round(final_score, 3),
                    projectTechStacks=project["techStacks"],
                    status=project["status"],
                    recruitDeadline=project["recruitDeadline"],
                    startDate=project["startDate"],
                    endDate=project["endDate"],
                    recruitCount=project["recruitCount"],
                    appliedCount=project["appliedCount"],
                    viewCount=project["viewCount"],
                    explanation=explanation
                )
                
                recommendations.append(recommendation)
        
        # 점수 기준으로 정렬하고 상위 N개 반환
        recommendations.sort(key=lambda x: x.matchScore, reverse=True)
        return recommendations[:top_n]

# 서비스 인스턴스 생성
project_recommendation_service = ProjectRecommendationService()