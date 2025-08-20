# app/ml/generate_training_data.py
"""
향상된 알고리즘으로 ML 훈련 데이터 생성
"""

import sys
import os

# 프로젝트 루트를 Python 경로에 추가
current_dir = os.path.dirname(os.path.abspath(__file__))
ai_server_dir = os.path.dirname(os.path.dirname(current_dir))
sys.path.insert(0, ai_server_dir)

import pandas as pd

from app.models import Project
from app.utils.feature_engineering import compute_features, enhanced_final_score
from app.utils.preprocess import normalize_tech_stacks, to_name_list

# 데이터베이스 연결 (기존 설정 사용)
from app.database import SessionLocal

# 다양한 사용자 기술스택 프로필 (훈련용)
USER_PROFILES = [
    # Frontend 개발자들
    {"name": "React 초급자", "techs": [{"name": "JavaScript", "level": 3}, {"name": "React", "level": 2}, {"name": "HTML", "level": 4}]},
    {"name": "React 중급자", "techs": [{"name": "JavaScript", "level": 4}, {"name": "React", "level": 4}, {"name": "TypeScript", "level": 3}, {"name": "Node.js", "level": 2}]},
    {"name": "React 고급자", "techs": [{"name": "JavaScript", "level": 5}, {"name": "React", "level": 5}, {"name": "TypeScript", "level": 4}, {"name": "Node.js", "level": 4}, {"name": "Redux", "level": 3}]},
    
    {"name": "Vue 초급자", "techs": [{"name": "JavaScript", "level": 3}, {"name": "Vue.js", "level": 2}, {"name": "HTML", "level": 4}]},
    {"name": "Vue 중급자", "techs": [{"name": "JavaScript", "level": 4}, {"name": "Vue.js", "level": 4}, {"name": "TypeScript", "level": 3}, {"name": "Nuxt.js", "level": 2}]},
    
    # Backend 개발자들
    {"name": "Python Django 개발자", "techs": [{"name": "Python", "level": 4}, {"name": "Django", "level": 4}, {"name": "PostgreSQL", "level": 3}, {"name": "Redis", "level": 2}]},
    {"name": "Python FastAPI 개발자", "techs": [{"name": "Python", "level": 4}, {"name": "FastAPI", "level": 4}, {"name": "SQLAlchemy", "level": 3}, {"name": "PostgreSQL", "level": 3}]},
    {"name": "Node.js 개발자", "techs": [{"name": "JavaScript", "level": 4}, {"name": "Node.js", "level": 4}, {"name": "Express", "level": 4}, {"name": "MongoDB", "level": 3}]},
    {"name": "Java Spring 개발자", "techs": [{"name": "Java", "level": 4}, {"name": "Spring Boot", "level": 4}, {"name": "PostgreSQL", "level": 3}, {"name": "Docker", "level": 2}]},
    
    # 풀스택 개발자들
    {"name": "MERN 스택", "techs": [{"name": "JavaScript", "level": 4}, {"name": "React", "level": 4}, {"name": "Node.js", "level": 4}, {"name": "MongoDB", "level": 3}, {"name": "Express", "level": 3}]},
    {"name": "PERN 스택", "techs": [{"name": "JavaScript", "level": 4}, {"name": "React", "level": 4}, {"name": "Node.js", "level": 4}, {"name": "PostgreSQL", "level": 3}, {"name": "Express", "level": 3}]},
    
    # 모바일 개발자들
    {"name": "React Native 개발자", "techs": [{"name": "JavaScript", "level": 4}, {"name": "React", "level": 4}, {"name": "React Native", "level": 4}, {"name": "TypeScript", "level": 3}]},
    {"name": "Flutter 개발자", "techs": [{"name": "Flutter", "level": 4}, {"name": "Dart", "level": 4}, {"name": "Firebase", "level": 3}, {"name": "SQLite", "level": 2}]},
    
    # DevOps/인프라
    {"name": "DevOps 엔지니어", "techs": [{"name": "Docker", "level": 4}, {"name": "Kubernetes", "level": 4}, {"name": "AWS", "level": 3}, {"name": "Terraform", "level": 3}]},
    
    # 게임/데이터 개발자
    {"name": "Unity 게임 개발자", "techs": [{"name": "Unity", "level": 4}, {"name": "C#", "level": 4}, {"name": "Blender", "level": 2}]},
    {"name": "ML 엔지니어", "techs": [{"name": "Python", "level": 4}, {"name": "TensorFlow", "level": 4}, {"name": "scikit-learn", "level": 3}, {"name": "Pandas", "level": 4}, {"name": "NumPy", "level": 4}]},
    
    # 초보자 프로필들
    {"name": "프로그래밍 입문자", "techs": [{"name": "Python", "level": 1}, {"name": "HTML", "level": 2}]},
    {"name": "웹개발 입문자", "techs": [{"name": "HTML", "level": 3}, {"name": "CSS", "level": 2}, {"name": "JavaScript", "level": 1}]},
    
    # 크로스 스킬 개발자들
    {"name": "Python + JS", "techs": [{"name": "Python", "level": 3}, {"name": "JavaScript", "level": 3}, {"name": "React", "level": 2}, {"name": "Django", "level": 2}]},
    {"name": "Java + React", "techs": [{"name": "Java", "level": 4}, {"name": "Spring Boot", "level": 3}, {"name": "JavaScript", "level": 3}, {"name": "React", "level": 3}]},
]

def generate_training_data():
    """향상된 알고리즘으로 훈련 데이터 생성"""
    
    # 데이터베이스에서 프로젝트 가져오기
    db = SessionLocal()
    try:
        db_projects = db.query(Project).all()
        print(f"데이터베이스에서 {len(db_projects)}개 프로젝트 로드")
        
        # 프로젝트 데이터 준비
        projects = []
        for p in db_projects:
            projects.append({
                "project_id": p.project_id,
                "title": p.title,
                "tech_stacks": [ts.tech_stack_name for ts in p.tech_stacks]
            })
        
        # 훈련 데이터 생성
        training_data = []
        
        for user_profile in USER_PROFILES:
            user_name = user_profile["name"]
            user_techs = user_profile["techs"]
            
            # 사용자 기술스택 정규화
            user_norm = normalize_tech_stacks(user_techs, min_level=1, max_level=5)
            user_names = to_name_list(user_norm)
            
            print(f"처리 중: {user_name} ({len(user_names)}개 기술)")
            
            for project in projects:
                proj_id = project["project_id"]
                proj_title = project["title"]
                proj_tech_stacks = project["tech_stacks"]
                
                # 프로젝트 기술스택 정규화 (기본 레벨 3)
                proj_raw = [{"name": name, "level": 3} for name in proj_tech_stacks]
                proj_norm = normalize_tech_stacks(proj_raw, min_level=1, max_level=5)
                proj_names = to_name_list(proj_norm)
                
                # 특성 벡터 계산 (ML 입력용)
                features = compute_features(user_names, proj_names)
                
                # 향상된 점수 계산 (ML 라벨용)
                score = enhanced_final_score(user_names, user_norm, proj_names, proj_norm)
                
                # 분류를 위한 라벨 생성 (추천할만함: 1, 그렇지 않음: 0)
                # 임계값 0.15 이상이면 추천할만함
                label = 1 if score >= 0.15 else 0
                
                training_data.append({
                    "user_profile": user_name,
                    "project_id": proj_id,
                    "project_title": proj_title,
                    "user_techs": user_names,
                    "project_techs": proj_names,
                    "features": features,
                    "enhanced_score": score,
                    "label": label
                })
        
        # DataFrame으로 변환
        df = pd.DataFrame(training_data)
        
        # 통계 출력
        print(f"\n=== 훈련 데이터 생성 완료 ===")
        print(f"총 샘플 수: {len(df)}")
        print(f"긍정 라벨 (추천할만함): {len(df[df['label'] == 1])}")
        print(f"부정 라벨 (추천 안함): {len(df[df['label'] == 0])}")
        print(f"평균 점수: {df['enhanced_score'].mean():.4f}")
        print(f"점수 분포:")
        print(df['enhanced_score'].describe())
        
        # CSV로 저장
        output_path = "/Users/jun/workspace/kosa-team-project-final/PR-Back/ai-server/training_data_enhanced.csv"
        df.to_csv(output_path, index=False)
        print(f"\n훈련 데이터가 저장되었습니다: {output_path}")
        
        return df
        
    finally:
        db.close()

if __name__ == "__main__":
    generate_training_data()