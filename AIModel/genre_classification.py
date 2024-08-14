import librosa
import numpy as np
from tensorflow import keras


def extract_features(audio_path):
    y, sr = librosa.load(audio_path)

    chroma_stft = librosa.feature.chroma_stft(y=y, sr=sr)
    chroma_stft_mean = np.mean(chroma_stft)
    chroma_stft_var = np.var(chroma_stft)

    rms = librosa.feature.rms(y=y)
    rms_mean = np.mean(rms)
    rms_var = np.var(rms)

    spectral_centroid = librosa.feature.spectral_centroid(y=y, sr=sr)
    spectral_centroid_mean = np.mean(spectral_centroid)
    spectral_centroid_var = np.var(spectral_centroid)

    spectral_bandwidth = librosa.feature.spectral_bandwidth(y=y, sr=sr)
    spectral_bandwidth_mean = np.mean(spectral_bandwidth)
    spectral_bandwidth_var = np.var(spectral_bandwidth)

    rolloff = librosa.feature.spectral_rolloff(y=y, sr=sr)
    rolloff_mean = np.mean(rolloff)
    rolloff_var = np.var(rolloff)

    zero_crossing_rate = librosa.feature.zero_crossing_rate(y)
    zero_crossing_rate_mean = np.mean(zero_crossing_rate)
    zero_crossing_rate_var = np.var(zero_crossing_rate)

    harmony = librosa.effects.harmonic(y)
    harmony_mean = np.mean(harmony)
    harmony_var = np.var(harmony)

    perceptr = librosa.effects.percussive(y)
    perceptr_mean = np.mean(perceptr)
    perceptr_var = np.var(perceptr)

    # tempo에서 첫 번째 값을 추출하여 사용
    tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
    tempo = tempo if isinstance(tempo, (int, float)) else tempo[0]

    mfccs = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=20)
    mfcc_means = [np.mean(mfcc) for mfcc in mfccs]
    mfcc_vars = [np.var(mfcc) for mfcc in mfccs]

    # 특성을 리스트로 추가
    feature_values = [
        chroma_stft_mean, chroma_stft_var,
        rms_mean, rms_var,
        spectral_centroid_mean, spectral_centroid_var,
        spectral_bandwidth_mean, spectral_bandwidth_var,
        rolloff_mean, rolloff_var,
        zero_crossing_rate_mean, zero_crossing_rate_var,
        harmony_mean, harmony_var,
        perceptr_mean, perceptr_var,
        tempo
    ] + mfcc_means + mfcc_vars

    return np.array(feature_values).reshape(1, -1)


audio_path = "audio_path"

features = extract_features(audio_path)
print(features)

model = keras.models.load_model('music_model.keras')

prediction = model.predict(features)

predicted_class = np.argmax(prediction, axis=1)
print(f"predicted class: {predicted_class[0]}")

# 클래스 이름을 인덱스에 매핑 (예시)
class_names = ['blues', 'classical', 'country', 'disco', 'hiphop', 'jazz', 'metal', 'pop', 'reggae', 'rock']

print(class_names[predicted_class[0]])


