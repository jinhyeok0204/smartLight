import librosa
import numpy as np
import xgboost
import time
import os


def extraction(file_path):
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

    # 8. Perceptual Features (e.g., Spectral Contrast)
    perceptr = librosa.feature.spectral_contrast(y=y, sr=sr)
    perceptr_mean = np.mean(perceptr)
    perceptr_var = np.var(perceptr)

    # 9. Tempo
    tempo, _ = librosa.beat.beat_track(y=y, sr=sr)

    # 10. MFCCs
    mfccs = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=20)
    mfcc_means = [np.mean(mfcc) for mfcc in mfccs]
    mfcc_vars = [np.var(mfcc) for mfcc in mfccs]

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

    return np.array(features)


my_model = xgboost.XGBClassifier()
my_model.load_model('xgb_model.h5')

for genre_folder in os.listdir('sliced_audio'):
    print(f"====================== {genre_folder} ==============================")
    count = 0
    for file_name in os.listdir(f"sliced_audio/{genre_folder}"):
        if count == 100:
            break
        features = extraction(f"sliced_audio/{genre_folder}/{file_name}")

        features = features.reshape(1, -1)

        prediction = my_model.predict(features)

        # Calculate predicted class and probability
        predicted_class = prediction[0]

        # Map class index to class name
        class_names = ['blues', 'classical', 'country', 'disco', 'hiphop', 'jazz', 'metal', 'pop', 'reggae', 'rock']

        # Print result
        print(f"Predicted class: {class_names[predicted_class]}")
        count += 1



