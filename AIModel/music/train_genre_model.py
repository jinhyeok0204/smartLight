import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from tensorflow import keras


df = pd.read_csv('features.csv')

print(df.dtypes)
# -------------------
class_list = df.iloc[:, -1]


convertor = LabelEncoder()

# fit_transform(): Fit label encoder and return encoded labels.
y = convertor.fit_transform(class_list)

print(f"class names: {convertor.classes_}")


fit = StandardScaler()
X = fit.fit_transform(np.array(df.iloc[:, :-1], dtype=float))

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.33)


def train_model(model, epochs, optimizer):
    batch_size = 128

    model.compile(optimizer=optimizer,
                  loss='sparse_categorical_crossentropy',
                  metrics=['accuracy'])
    return model.fit(X_train, y_train, validation_data=(X_test, y_test), batch_size=batch_size, epochs=epochs)


def plot_validate(history):
    print("Validation Accuracy", max(history.history["val_accuracy"]))
    pd.DataFrame(history.history).plot(figSize=(12, 6))
    plt.show()


model = keras.models.Sequential([
    keras.layers.Dense(256, input_shape=(X_train.shape[1],), activation='relu'),
    keras.layers.BatchNormalization(),
    keras.layers.Dropout(0.3),

    keras.layers.Dense(128, activation='relu'),
    keras.layers.BatchNormalization(),
    keras.layers.Dropout(0.3),

    keras.layers.Dense(64, activation='relu'),
    keras.layers.BatchNormalization(),
    keras.layers.Dropout(0.3),

    keras.layers.Dense(32, activation='relu'),
    keras.layers.BatchNormalization(),
    keras.layers.Dropout(0.3),

    keras.layers.Dense(10, activation='softmax')
])

print(model.summary())
model_history = train_model(model=model, epochs=200, optimizer='adam')

test_loss, test_acc = model.evaluate(X_test, y_test, batch_size=32)
print("The test Loss is : ", test_loss)
print("\nThe Best Test Accuracy is : ", test_acc * 100)

model_path = "music_model.h5"
model.save(model_path)
print("Model saved")

# 모델 로드
loaded_model = keras.models.load_model(model_path)
predictions = loaded_model.predict(X_test)