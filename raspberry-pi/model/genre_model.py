# Usual Libraries
import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
import sklearn

# Librosa (the mother of audio files)
import librosa
import librosa.display
import IPython.display as ipd
import warnings
warnings.filterwarnings('ignore')

import os
import joblib  # Add this import

from sklearn.model_selection import GridSearchCV


general_path = '../Data'
print(list(os.listdir(f'{general_path}/genres_original/')))

from xgboost import XGBClassifier, XGBRFClassifier

from sklearn.metrics import confusion_matrix, accuracy_score, roc_auc_score, roc_curve
from sklearn import preprocessing
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder  # Add this import

#data = pd.read_csv(f'{general_path}/features_3_sec.csv')
data = pd.read_csv("../myfeatures.csv")
data = data.iloc[0:, 1:]
print(data.head())

y = data['label'] # genre variable.
X = data.loc[:, data.columns != 'label'] #select all columns but not the labels

#### NORMALIZE X ####

# Normalize so everything is on the same scale.

cols = X.columns
min_max_scaler = preprocessing.MinMaxScaler()
np_scaled = min_max_scaler.fit_transform(X)

# new data frame with the new scaled data.
X = pd.DataFrame(np_scaled, columns=cols)

# Encode labels
label_encoder = LabelEncoder()
y_encoded = label_encoder.fit_transform(y)

print(y_encoded)
X_train, X_test, y_train, y_test = train_test_split(X, y_encoded, test_size=0.2, random_state=42)

print(X_test)

def model_assess(model, title = "Default"):
    model.fit(X_train, y_train)
    preds = model.predict(X_test)
    #print(confusion_matrix(y_test, preds))
    print('Accuracy', title, ':', round(accuracy_score(y_test, preds), 5), '\n')

# Cross Gradient Booster
xgb = XGBClassifier(n_estimators=1000, learning_rate=0.05)
model_assess(xgb, "Cross Gradient Booster")

# Save the model to a file
model_filename = f'models/my_genre_model.pkl'
joblib.dump(xgb, model_filename)
print(f"Model saved to {model_filename}")

# Load the model from the file
loaded_model = joblib.load(model_filename)

# Use the loaded model to make predictions
loaded_preds = loaded_model.predict(X_test)
print('Loaded Model Accuracy:', round(accuracy_score(y_test, loaded_preds), 5))



# 파라미터 그리드 정의
param_grid = {
    'n_estimators': [100, 500, 1000],
    'learning_rate': [0.01, 0.05, 0.1],
    'max_depth': [3, 5, 7],
    'subsample': [0.8, 1.0],
    'colsample_bytree': [0.8, 1.0]
}

# XGBoost 분류기 초기화
xgb = XGBClassifier()

# GridSearchCV 객체 초기화
grid_search = GridSearchCV(estimator=xgb, param_grid=param_grid, cv=3, scoring='accuracy', verbose=1, n_jobs=-1)

# GridSearchCV 객체를 데이터에 맞추기
grid_search.fit(X_train, y_train)

# 최적의 파라미터 얻기
best_params = grid_search.best_params_
print(f"최적의 파라미터: {best_params}")

# 최적의 파라미터로 모델 훈련
best_xgb = XGBClassifier(**best_params)
model_assess(best_xgb, "튜닝된 Cross Gradient Booster")

# 튜닝된 모델 파일로 저장
tuned_model_filename = f'models/my_tuned_genre_model.pkl'
joblib.dump(best_xgb, tuned_model_filename)
print(f"튜닝된 모델이 {tuned_model_filename}에 저장되었습니다")

# 파일에서 튜닝된 모델 불러오기
loaded_tuned_model = joblib.load(tuned_model_filename)

# 불러온 튜닝된 모델로 예측하기
loaded_tuned_preds = loaded_tuned_model.predict(X_test)
print('불러온 튜닝된 모델의 정확도:', round(accuracy_score(y_test, loaded_tuned_preds), 5))





















