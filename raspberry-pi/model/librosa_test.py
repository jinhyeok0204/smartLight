import librosa
import numpy as np
import pandas as pd
from pydub import AudioSegment

audio_file = "../Data/genres_original/blues/blues.00000.wav"

segment_length = 3000 # 3초

output_folder = "segment"
song = AudioSegment.from_file(audio_file)
total_length = len(song)
for i in range(0, total_length, segment_length):
    segment = song[i:i+segment_length]
    segment.export(f"{output_folder}/{i}.wav", format="wav")

print("segmentation end")

y, sr = librosa.load(f"./segment/{0}.wav")

spectral_centroid_mean = np.mean(librosa.feature.spectral_centroid(y=y, sr=sr))
spectral_centroid_var = np.var(librosa.feature.spectral_centroid(y=y, sr=sr))
spectral_bandwidth_mean = np.mean(librosa.feature.spectral_bandwidth(y=y, sr=sr))
spectral_bandwidth_var = np.var(librosa.feature.spectral_bandwidth(y=y, sr=sr))
rolloff_mean = np.mean(librosa.feature.spectral_rolloff(y=y, sr=sr))
rolloff_var = np.var(librosa.feature.spectral_rolloff(y=y, sr=sr))
zero_crossing_rate_mean = np.mean(librosa.feature.zero_crossing_rate(y=y))
zero_crossing_rate_var = np.var(librosa.feature.zero_crossing_rate(y=y))
tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
mfccs = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=20)
mfccs_mean = np.mean(mfccs, axis=1)
mfccs_var = np.var(mfccs, axis=1)

print(mfccs_mean)


# 값들이 Data/featues_3_sec.csv와 비슷한 것 확인할 수 있음.

