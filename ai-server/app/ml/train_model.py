from sklearn.ensemble import RandomForestClassifier
import joblib

model = RandomForestClassifier()
model.fit(X_train, y_train)
joblib.dump(model, "model/recommender.pkl")