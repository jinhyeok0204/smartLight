import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Conv2D, MaxPooling2D, Flatten, Dense, Dropout
from sklearn.metrics import classification_report, confusion_matrix

# 1. CSV 파일 불러오기 및 데이터 전처리

# CSV 파일 불러오기
csv_file = 'features.csv'
data = pd.read_csv(csv_file)

# 특성과 라벨 분리
X = data.drop(columns=['genre'])
y = data['genre']

# 라벨 인코딩
label_encoder = LabelEncoder()
y_encoded = label_encoder.fit_transform(y)

# 데이터 스케일링
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# 데이터를 CNN 입력에 맞게 3D 형태로 변환 (samples, 17, 1, 1)
X_cnn = X_scaled.reshape(X_scaled.shape[0], 17, 1, 1)

# 라벨을 원-핫 인코딩
y_categorical = to_categorical(y_encoded)

# 학습/테스트 데이터 분할
X_train, X_test, y_train, y_test = train_test_split(X_cnn, y_categorical, test_size=0.2, random_state=42)

# 2. CNN 모델 구축 및 학습

# CNN 모델 구축
model = Sequential()
model.add(Conv2D(32, kernel_size=(3, 3), activation='relu', input_shape=(17, 1, 1)))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Dropout(0.3))

model.add(Conv2D(64, kernel_size=(3, 3), activation='relu'))
model.add(MaxPooling2D(pool_size=(2, 2)))
model.add(Dropout(0.3))

model.add(Flatten())
model.add(Dense(128, activation='relu'))
model.add(Dropout(0.4))
model.add(Dense(len(label_encoder.classes_), activation='softmax'))

# 모델 컴파일
model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

# 모델 학습
model.fit(X_train, y_train, epochs=30, batch_size=32, validation_data=(X_test, y_test))

# 3. 모델 평가

# 테스트 데이터에 대한 예측
y_pred = model.predict(X_test)
y_pred_classes = np.argmax(y_pred, axis=1)
y_test_classes = np.argmax(y_test, axis=1)

# 혼동 행렬 및 분류 보고서 출력
print(confusion_matrix(y_test_classes, y_pred_classes))
print(classification_report(y_test_classes, y_pred_classes, target_names=label_encoder.classes_))
