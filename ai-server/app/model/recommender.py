import os
import joblib
from sklearn.ensemble import RandomForestClassifier

# 학습용 더미 데이터 (간단한 예시)
# X는 [overlap 비율], y는 프로젝트 매칭 여부 (0: 불일치, 1: 일치)
X_train = [[0.1], [0.3], [0.5], [0.7], [0.9]]
y_train = [0, 0, 1, 1, 1]

# 모델 정의 및 학습
model = RandomForestClassifier(n_estimators=100, random_state=42)
model.fit(X_train, y_train)

# 모델 저장 경로 생성
os.makedirs("model", exist_ok=True)

# 모델 저장
joblib.dump(model, "model/recommender.pkl")

print("✅ 모델 학습 및 저장 완료: model/recommender.pkl")