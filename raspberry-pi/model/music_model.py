import os
import librosa
import numpy as np
import pandas as pd

def create_features_file():
    music_dir = '../songs'
    temp_dir = '../temp'

    # 임시 디렉토리 생성
    if not os.path.exists(temp_dir):
        os.makedirs(temp_dir)


    # 특징을 추출할 음악 파일들의 목록
    audio_files = [f for f in os.listdir(music_dir) if f.endswith('.mp3')]

    # 특징을 저장할 리스트
    features_list = []

    # 각 음악 파일에 대해 특징 추출
    for audio_file in audio_files:
        input_path = os.path.join(music_dir, audio_file)


        if len(features_list) == 100:
            break

        y, sr = librosa.load(input_path)

        features = extract_features(y, sr)
        features['file_name'] = audio_file

        features_list.append(features)
        print(len(features_list))

    df = pd.DataFrame(features_list)
    df.to_csv('music_features.csv', index=False)


def extract_features(y, sr):
    # 특징 추출
    spectral_centroid_mean = np.mean(librosa.feature.spectral_centroid(y=y, sr=sr))
    spectral_centroid_var = np.var(librosa.feature.spectral_centroid(y=y, sr=sr))
    spectral_bandwidth_mean = np.mean(librosa.feature.spectral_bandwidth(y=y, sr=sr))
    spectral_bandwidth_var = np.var(librosa.feature.spectral_bandwidth(y=y, sr=sr))
    rolloff_mean = np.mean(librosa.feature.spectral_rolloff(y=y, sr=sr))
    rolloff_var = np.var(librosa.feature.spectral_rolloff(y=y, sr=sr))
    zero_crossing_rate_mean = np.mean(librosa.feature.zero_crossing_rate(y=y))
    zero_crossing_rate_var = np.var(librosa.feature.zero_crossing_rate(y=y))
    tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=20)
    mfcc_mean = np.mean(mfcc, axis=1)
    mfcc_var = np.var(mfcc, axis=1)

    # 데이터 정리
    data = {
        'spectral_centroid_mean': spectral_centroid_mean, 'spectral_centroid_var': spectral_centroid_var,
        'spectral_bandwidth_mean': spectral_bandwidth_mean, 'spectral_bandwidth_var': spectral_bandwidth_var,
        'rolloff_mean': rolloff_mean, 'rolloff_var': rolloff_var,
        'zero_crossing_rate_mean': zero_crossing_rate_mean, 'zero_crossing_rate_var': zero_crossing_rate_var,
        'tempo': tempo,
    }

    # mfcc 값 추가
    for i in range(1, 21):
        data[f'mfcc{i}_mean'] = mfcc_mean[i - 1]
        data[f'mfcc{i}_var'] = mfcc_var[i - 1]

    return data

#============= 음악 특징 파일 추출 ============================


def main():
    file = "music_features.csv"

    create_features_file()


main()