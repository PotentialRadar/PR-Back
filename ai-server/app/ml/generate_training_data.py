# app/ml/generate_training_data.py
"""
향상된 알고리즘으로 ML 훈련 데이터 생성

이 모듈은 실제 데이터베이스의 사용자 기술스택 정보와 프로젝트 정보를 기반으로
머신러닝 훈련 데이터를 생성합니다.

주요 기능:
- 실제 DB에서 사용자 기술스택 조회
- 프로젝트별 기술요구사항과 사용자 스킬 매칭
- 훈련용 특성 벡터 및 라벨 생성
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

# 훈련 데이터는 실제 데이터베이스의 사용자 정보를 사용합니다.

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
                "tech_stacks": [ts.tech_stack.name for ts in p.tech_stacks]
            })
        
        # 실제 DB 사용자 기술스택 가져오기 (Raw SQL 사용)
        user_tech_query = """
        SELECT u.user_id, u.email, ts.name as tech_name, uts.skill_level
        FROM users u 
        JOIN user_tech_stack uts ON u.user_id = uts.user_id
        JOIN tech_stack ts ON uts.stack_id = ts.tech_stack_id
        ORDER BY u.user_id
        """
        
        from sqlalchemy import text
        result = db.execute(text(user_tech_query)).fetchall()
        
        # 사용자별로 기술스택 그룹핑
        user_data = {}
        for row in result:
            user_id = row.user_id
            if user_id not in user_data:
                user_data[user_id] = {"email": row.email, "techs": []}
            user_data[user_id]["techs"].append({"name": row.tech_name, "level": row.skill_level})
        
        print(f"데이터베이스에서 {len(user_data)}명의 사용자 로드")
        
        # 훈련 데이터 생성
        training_data = []
        
        for user_id, user_info in user_data.items():
            user_name = f"User_{user_id} ({user_info['email']})"
            user_techs = user_info["techs"]
            
            if not user_techs:  # 기술스택이 없는 사용자는 건너뛰기
                continue
            
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