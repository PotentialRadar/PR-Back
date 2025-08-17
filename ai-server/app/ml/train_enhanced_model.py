# app/ml/train_enhanced_model.py
"""
향상된 알고리즘 기반 ML 모델 훈련
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score
import joblib
import os

def train_enhanced_model():
    """향상된 알고리즘으로 생성된 데이터로 ML 모델 훈련"""
    from app.config import get_training_data_path
    
    # 훈련 데이터 로드
    data_path = get_training_data_path()
    df = pd.read_csv(data_path)
    
    print(f"훈련 데이터 로드: {len(df)}개 샘플")
    print(f"긍정 라벨: {len(df[df['label'] == 1])}개")
    print(f"부정 라벨: {len(df[df['label'] == 0])}개")
    
    # 특성과 라벨 분리
    # features 컬럼은 리스트 형태의 문자열이므로 파싱 필요
    X = []
    y = df['label'].values
    
    for i, row in df.iterrows():
        # features 컬럼은 "[0.33333]" 형태의 문자열
        features_str = row['features']
        # 문자열에서 숫자 추출
        features_str = features_str.strip('[]')
        features = [float(x) for x in features_str.split(',')]
        X.append(features)
    
    X = np.array(X)
    
    print(f"특성 차원: {X.shape}")
    print(f"라벨 분포: {np.bincount(y)}")
    
    # 훈련/테스트 데이터 분할
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    
    print(f"훈련 셋: {len(X_train)}개")
    print(f"테스트 셋: {len(X_test)}개")
    
    # 모델 훈련 (RandomForest 사용)
    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        min_samples_split=5,
        min_samples_leaf=2,
        random_state=42,
        class_weight='balanced'  # 불균형 데이터 처리
    )
    
    print("\n모델 훈련 중...")
    model.fit(X_train, y_train)
    
    # 교차 검증
    cv_scores = cross_val_score(model, X_train, y_train, cv=5, scoring='roc_auc')
    print(f"교차 검증 AUC: {cv_scores.mean():.4f} (+/- {cv_scores.std() * 2:.4f})")
    
    # 테스트 셋 평가
    y_pred = model.predict(X_test)
    y_pred_proba = model.predict_proba(X_test)[:, 1]
    
    print(f"\n=== 테스트 셋 성능 ===")
    print(f"AUC Score: {roc_auc_score(y_test, y_pred_proba):.4f}")
    print(f"\nClassification Report:")
    print(classification_report(y_test, y_pred))
    print(f"\nConfusion Matrix:")
    print(confusion_matrix(y_test, y_pred))
    
    # 특성 중요도
    print(f"\n특성 중요도:")
    feature_names = ['jaccard_overlap']  # 현재는 Jaccard overlap 1개만 사용
    for i, importance in enumerate(model.feature_importances_):
        print(f"{feature_names[i]}: {importance:.4f}")
    
    # 모델 저장
    from app.config import get_model_path
    from pathlib import Path
    
    model_path = Path(get_model_path())
    model_dir = model_path.parent
    
    # 향상된 모델 저장
    enhanced_model_path = model_dir / "recommender_enhanced.pkl"
    joblib.dump(model, enhanced_model_path)
    print(f"\n향상된 모델이 저장되었습니다: {enhanced_model_path}")
    
    # 기존 모델 백업하고 새 모델로 교체
    backup_path = model_dir / "recommender_backup.pkl"
    
    if model_path.exists():
        model_path.rename(backup_path)
        print(f"기존 모델을 백업했습니다: {backup_path}")
    
    # 새 모델을 기본 모델로 설정
    import shutil
    shutil.copy2(enhanced_model_path, model_path)
    print(f"새 모델을 기본 모델로 설정했습니다: {model_path}")
    
    # 샘플 예측 테스트
    print(f"\n=== 샘플 예측 테스트 ===")
    
    # 높은 점수 샘플 몇 개 찾기
    high_score_samples = df[df['enhanced_score'] > 0.3]
    if len(high_score_samples) > 0:
        sample = high_score_samples.iloc[0]
        print(f"사용자: {sample['user_profile']}")
        print(f"프로젝트: {sample['project_title']}")
        print(f"실제 점수: {sample['enhanced_score']:.4f}")
        
        # 특성 추출
        features_str = sample['features'].strip('[]')
        features = [[float(x) for x in features_str.split(',')]]
        
        pred_proba = model.predict_proba(features)[0]
        print(f"ML 예측 확률: {pred_proba[1]:.4f}")
        print(f"ML 예측 라벨: {model.predict(features)[0]}")
    
    return model

if __name__ == "__main__":
    train_enhanced_model()