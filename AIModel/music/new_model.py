import pandas as pd
import numpy as np
import librosa
import os

genres = ['blues', 'classical', 'country', 'disco', 'hiphop', 'jazz', 'metal', 'pop', 'reggae', 'rock']
data_dir = 'genres_original/'


def extract_features(audio_path):
    try:
        y, sr = librosa.load(audio_path)

        chroma_stft = librosa.feature.chroma_stft(y=y, sr=sr)
        chroma_stft_mean = np.mean(chroma_stft)
        #chroma_stft_var = np.var(chroma_stft)

        rms = librosa.feature.rms(y=y)
        rms_mean = np.mean(rms)
        #rms_var = np.var(rms)

        spectral_centroid = librosa.feature.spectral_centroid(y=y, sr=sr)
        spectral_centroid_mean = np.mean(spectral_centroid)
        #spectral_centroid_var = np.var(spectral_centroid)


        zero_crossing_rate = librosa.feature.zero_crossing_rate(y)
        zero_crossing_rate_mean = np.mean(zero_crossing_rate)
        #zero_crossing_rate_var = np.var(zero_crossing_rate)

        harmony = librosa.effects.harmonic(y)
        harmony_mean = np.mean(harmony)
        #harmony_var = np.var(harmony)

        perceptr = librosa.effects.percussive(y)
        perceptr_mean = np.mean(perceptr)
        #perceptr_var = np.var(perceptr)

        # tempo에서 첫 번째 값을 추출하여 사용
        tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
        tempo = tempo if isinstance(tempo, (int, float)) else tempo[0]

        # MFCC를 제외한 특성을 리스트로 추가
        feature_values = [
            chroma_stft_mean,
            rms_mean,
            spectral_centroid_mean,
            zero_crossing_rate_mean,
            harmony_mean,
            perceptr_mean,
            tempo
        ]

        return feature_values
    except Exception as e:
        print(f"Error loading {audio_path} : {e}")
        return None


features = []
labels = []

# 모든 파일에서 특징 추출
for genre in genres:
    genre_dir = os.path.join(data_dir, genre)
    print(genre_dir)
    for file_name in os.listdir(genre_dir):
        file_path = os.path.join(genre_dir, file_name)

        feature = extract_features(file_path)
        if feature is not None:
            features.append(feature)
            labels.append(genre)

feature_columns = [
    "chroma_stft_mean",
    "rms_mean",
    "spectral_centroid_mean",
    "zero_crossing_rate_mean",
    "harmony_mean",
    "perceptr_mean",
    "tempo"
]
features = pd.DataFrame(features)
labels_df = pd.Series(labels, name='genre')

final_df = pd.concat([features, labels_df], axis=1)

csv_file = 'features.csv'
final_df.to_csv(csv_file, index=False)

print("----------saved csv file------------")






