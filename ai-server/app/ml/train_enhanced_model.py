# app/ml/train_enhanced_model.py
"""
머신러닝 모델 훈련 모듈

사용자-프로젝트 매칭을 예측하는 ML 모델을 훈련합니다.
훈련 데이터는 실제 DB의 사용자 기술스택과 프로젝트 정보를 기반으로 생성됩니다.
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score
import joblib
import os

def train_enhanced_model():
    """
    머신러닝 모델 훈련 메인 함수
    
    과정:
    1. 훈련 데이터 로드 (CSV 파일에서)
    2. 특성과 라벨 분리 및 전처리
    3. 훈련/테스트 데이터 분할
    4. RandomForest 모델 훈련
    5. 모델 성능 평가
    6. 훈련된 모델 저장
    """
    from app.config import get_training_data_path
    
    # === 1. 훈련 데이터 로드 ===
    # CSV 파일에는 사용자-프로젝트 조합과 추천 여부(라벨)가 저장되어 있음
    data_path = get_training_data_path()
    df = pd.read_csv(data_path)
    
    print(f"훈련 데이터 로드: {len(df)}개 샘플")
    print(f"긍정 라벨 (추천함): {len(df[df['label'] == 1])}개")
    print(f"부정 라벨 (추천 안함): {len(df[df['label'] == 0])}개")
    
    # === 2. 특성과 라벨 분리 ===
    # 특성(X): 판단 기준 (현재는 기술스택 겹치는 비율만 사용)
    # 라벨(y): 정답 (추천해야 하는가? 1=예, 0=아니오)
    X = []  # 특성 배열 (입력)
    y = df['label'].values  # 라벨 배열 (출력/정답)
    
    # CSV에서 특성 데이터를 문자열로 저장했으므로 숫자로 변환 필요
    for i, row in df.iterrows():
        # "[0.33333]" 형태의 문자열을 [0.33333] 숫자 배열로 변환
        features_str = row['features']
        features_str = features_str.strip('[]')  # 대괄호 제거
        features = [float(x) for x in features_str.split(',')]  # 숫자 배열로 변환
        X.append(features)
    
    X = np.array(X)  # NumPy 배열로 변환 (scikit-learn에서 요구)
    
    print(f"특성 차원: {X.shape}")  # (샘플 수, 특성 수)
    print(f"라벨 분포: {np.bincount(y)}")  # [부정 라벨 개수, 긍정 라벨 개수]
    
    # === 3. 훈련/테스트 데이터 분할 ===
    # 80%는 모델 훈련용, 20%는 성능 평가용으로 분할
    # stratify=y: 긍정/부정 라벨 비율을 훈련/테스트 셋에서 동일하게 유지
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    
    print(f"훈련 셋: {len(X_train)}개 (모델 학습용)")
    print(f"테스트 셋: {len(X_test)}개 (성능 평가용)")
    
    # === 4. 모델 생성 및 훈련 ===
    # RandomForest: 여러 개의 의사결정트리를 조합한 모델
    model = RandomForestClassifier(
        n_estimators=100,        # 트리 100개 사용 (더 많을수록 정확하지만 느림)
        max_depth=10,           # 트리 깊이 제한 (과적합 방지)
        min_samples_split=5,    # 노드를 분할하는 최소 샘플 수
        min_samples_leaf=2,     # 잎 노드의 최소 샘플 수
        random_state=42,        # 재현 가능한 결과를 위한 시드값
        class_weight='balanced' # 긍정/부정 라벨 불균형 자동 보정
    )
    
    print("\n모델 훈련 중...")
    # 훈련 데이터로 모델 학습: "특성 → 라벨" 패턴 학습
    model.fit(X_train, y_train)
    
    # === 5. 모델 성능 평가 ===
    
    # 5.1 교차 검증 (Cross Validation)
    # 훈련 데이터를 5개 부분으로 나누어 여러 번 검증
    cv_scores = cross_val_score(model, X_train, y_train, cv=5, scoring='roc_auc')
    print(f"교차 검증 AUC: {cv_scores.mean():.4f} (+/- {cv_scores.std() * 2:.4f})")
    
    # 5.2 테스트 셋으로 최종 성능 평가
    y_pred = model.predict(X_test)  # 예측 라벨 (0 또는 1)
    y_pred_proba = model.predict_proba(X_test)[:, 1]  # 추천 확률 (0.0~1.0)
    
    print(f"\n=== 테스트 셋 성능 ===")
    # AUC: 모델이 얼마나 잘 구분하는지 (1.0에 가까울수록 좋음)
    print(f"AUC Score: {roc_auc_score(y_test, y_pred_proba):.4f}")
    
    # 상세 성능 리포트 (정밀도, 재현율, F1-score)
    print(f"\nClassification Report:")
    print(classification_report(y_test, y_pred))
    
    # 혼동 행렬: 실제 vs 예측 결과 매트릭스
    print(f"\nConfusion Matrix:")
    print(confusion_matrix(y_test, y_pred))
    
    # === 6. 특성 중요도 확인 ===
    # 어떤 특성이 예측에 가장 중요한지 확인
    print(f"\n특성 중요도:")
    feature_names = ['jaccard_overlap']  # 현재는 기술스택 겹치는 비율 1개만 사용
    for i, importance in enumerate(model.feature_importances_):
        print(f"{feature_names[i]}: {importance:.4f} (1.0에 가까울수록 중요)")
    
    # === 7. 모델 저장 및 배포 ===
    from app.config import get_model_path
    from pathlib import Path
    
    model_path = Path(get_model_path())
    model_dir = model_path.parent
    
    # 새로 훈련된 모델 저장
    enhanced_model_path = model_dir / "recommender_enhanced.pkl"
    joblib.dump(model, enhanced_model_path)
    print(f"\n새 모델 저장: {enhanced_model_path}")
    
    # 기존 모델이 있다면 백업
    backup_path = model_dir / "recommender_backup.pkl"
    if model_path.exists():
        model_path.rename(backup_path)
        print(f"기존 모델 백업: {backup_path}")
    
    # 새 모델을 메인 모델로 설정 (추천 API에서 사용)
    import shutil
    shutil.copy2(enhanced_model_path, model_path)
    print(f"새 모델을 메인 모델로 설정: {model_path}")
    
    # === 8. 샘플 예측 테스트 ===
    print(f"\n=== 샘플 예측 테스트 ===")
    
    # 높은 점수를 받은 샘플로 테스트
    high_score_samples = df[df['enhanced_score'] > 0.3]
    if len(high_score_samples) > 0:
        sample = high_score_samples.iloc[0]
        print(f"테스트 사용자: {sample['user_profile']}")
        print(f"테스트 프로젝트: {sample['project_title']}")
        print(f"실제 계산된 점수: {sample['enhanced_score']:.4f}")
        
        # 모델 예측 수행
        features_str = sample['features'].strip('[]')
        features = [[float(x) for x in features_str.split(',')]]
        
        pred_proba = model.predict_proba(features)[0]
        pred_label = model.predict(features)[0]
        
        print(f"ML 모델 예측 확률: {pred_proba[1]:.4f} (추천할 확률)")
        print(f"ML 모델 예측 결과: {'추천함' if pred_label == 1 else '추천 안함'}")
    
    return model

if __name__ == "__main__":
    train_enhanced_model()