import librosa
import os
import numpy as np
import pandas as pd
from scipy.io import wavfile


# def slice_audio_file(file_path, output_dir, slice_duration=3):
#     y, sr = librosa.load(file_path)
#
#     total_duration = librosa.get_duration(y = y, sr= sr)
#
#     # 슬라이스 개수
#
#     num_slices = int(total_duration // slice_duration)
#
#     for i in range(num_slices):
#         start_sample = int(i * slice_duration * sr)
#         end_sample = int((i + 1) * slice_duration * sr)
#         slice_y = y[start_sample:end_sample]
#
#         output_file = os.path.join(output_dir, f"{os.path.splitext(os.path.basename(file_path))[0]}_slice_{i}.wav")
#         wavfile.write(output_file, sr, slice_y)
#
#
#
# input_dir = "genres_original"
# output_dir = "sliced_audio"
#
# # output_dir 폴더가 없으면 생성
# os.makedirs(output_dir, exist_ok=True)
#
# print(os.listdir(input_dir))
#
# for genre_folder in os.listdir(input_dir):
#     genre_path = os.path.join(input_dir, genre_folder)
#     genre_output_path = os.path.join(output_dir, genre_folder)
#
#     # 장르별 출력 폴더 생성
#     os.makedirs(genre_output_path, exist_ok=True)
#
#     for file_name in os.listdir(genre_path):
#         file_path = os.path.join(genre_path, file_name)
#         try:
#             slice_audio_file(file_path, genre_output_path)
#         except Exception as e:
#             print(e)

def extraction(file_path):
    features = []

    y, sr = librosa.load(file_path)
    # 1. Chroma STFT
    chroma_stft = librosa.feature.chroma_stft(y=y, sr=sr)
    chroma_stft_mean = np.mean(chroma_stft)
    chroma_stft_var = np.var(chroma_stft)

    # 2. RMS (Root Mean Square)
    rms = librosa.feature.rms(y=y)
    rms_mean = np.mean(rms)
    rms_var = np.var(rms)

    # 3. Spectral Centroid
    spectral_centroid = librosa.feature.spectral_centroid(y=y, sr=sr)
    spectral_centroid_mean = np.mean(spectral_centroid)
    spectral_centroid_var = np.var(spectral_centroid)

    # 4. Spectral Bandwidth
    spectral_bandwidth = librosa.feature.spectral_bandwidth(y=y, sr=sr)
    spectral_bandwidth_mean = np.mean(spectral_bandwidth)
    spectral_bandwidth_var = np.var(spectral_bandwidth)

    # 5. Spectral Rolloff
    rolloff = librosa.feature.spectral_rolloff(y=y, sr=sr)
    rolloff_mean = np.mean(rolloff)
    rolloff_var = np.var(rolloff)

    # 6. Zero Crossing Rate
    zero_crossing_rate = librosa.feature.zero_crossing_rate(y)
    zero_crossing_rate_mean = np.mean(zero_crossing_rate)
    zero_crossing_rate_var = np.var(zero_crossing_rate)

    # 7. Harmony
    harmony = librosa.effects.harmonic(y)
    harmony_mean = np.mean(harmony)
    harmony_var = np.var(harmony)

    # 8. Perceptr (예: Perceptual CQT, Harmonic 등)
    perceptr = librosa.feature.spectral_contrast(y=y, sr=sr)
    perceptr_mean = np.mean(perceptr)
    perceptr_var = np.var(perceptr)

    # 9. Tempo
    tempo, _ = librosa.beat.beat_track(y=y, sr=sr)

    # 10. MFCCs
    mfccs = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=20)
    mfcc_means = [np.mean(mfcc) for mfcc in mfccs]
    mfcc_vars = [np.var(mfcc) for mfcc in mfccs]

    print(mfcc_means)
    print(len(mfcc_means), len(mfcc_vars))

    features = [
        chroma_stft_mean, chroma_stft_var,
        rms_mean, rms_var,
        spectral_centroid_mean, spectral_centroid_var,
        rolloff_mean, rolloff_var,
        zero_crossing_rate_mean, zero_crossing_rate_var,
        harmony_mean, harmony_var,
        perceptr_mean, perceptr_var,
        tempo[0], *mfcc_means, *mfcc_vars,
    ]
    #print(features)
    return features


all_features = []
for genre_folder in os.listdir('sliced_audio'):
    folder_path = os.path.join('sliced_audio', genre_folder)
    print(folder_path)
    for file_name in os.listdir(folder_path):
        features = extraction(os.path.join('sliced_audio', genre_folder, file_name))
        features.append(genre_folder)
        all_features.append(features)

# output_csv = "music_features.csv"
#
# df = pd.DataFrame(all_features)
# df.to_csv(output_csv, index=False)


