import pyaudio
import numpy as np
import librosa
import xgboost

# PyAudio 초기화
p = pyaudio.PyAudio()

# 오디오 스트림 설정
CHUNK = 1024  # 버퍼 크기
FORMAT = pyaudio.paInt16  # 오디오 포맷
CHANNELS = 1  # 모노 오디오
RATE = 44100  # 샘플링 레이트 (Hz)
DURATION = 4  # 분석할 오디오의 길이 (초)

# XGBoost 모델 로드
model = xgboost.XGBClassifier()
model.load_model('xgb_model.h5')

class_names = ['blues', 'classical', 'country', 'disco', 'hiphop', 'jazz', 'metal', 'pop', 'reggae', 'rock']

def extraction(audio_data, sr):
    # int16 데이터를 float32로 변환하고, -1.0 ~ 1.0 범위로 정규화
    audio_data = audio_data.astype(np.float32)

    # 1. Chroma STFT
    chroma_stft = librosa.feature.chroma_stft(y=audio_data, sr=sr)
    chroma_stft_mean = np.mean(chroma_stft)
    chroma_stft_var = np.var(chroma_stft)

    # 2. RMS (Root Mean Square)
    rms = librosa.feature.rms(y=audio_data)
    rms_mean = np.mean(rms)
    rms_var = np.var(rms)

    # 3. Spectral Centroid
    spectral_centroid = librosa.feature.spectral_centroid(y=audio_data, sr=sr)
    spectral_centroid_mean = np.mean(spectral_centroid)
    spectral_centroid_var = np.var(spectral_centroid)

    # 5. Spectral Rolloff
    rolloff = librosa.feature.spectral_rolloff(y=audio_data, sr=sr)
    rolloff_mean = np.mean(rolloff)
    rolloff_var = np.var(rolloff)

    # 6. Zero Crossing Rate
    zero_crossing_rate = librosa.feature.zero_crossing_rate(audio_data)
    zero_crossing_rate_mean = np.mean(zero_crossing_rate)
    zero_crossing_rate_var = np.var(zero_crossing_rate)

    # 7. Harmony
    harmony = librosa.effects.harmonic(audio_data)
    harmony_mean = np.mean(harmony)
    harmony_var = np.var(harmony)

    # 8. Perceptual Features (e.g., Spectral Contrast)
    perceptr = librosa.feature.spectral_contrast(y=audio_data, sr=sr)
    perceptr_mean = np.mean(perceptr)
    perceptr_var = np.var(perceptr)

    # 9. Tempo
    tempo, _ = librosa.beat.beat_track(y=audio_data, sr=sr)

    # 10. MFCCs
    mfccs = librosa.feature.mfcc(y=audio_data, sr=sr, n_mfcc=20)
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


# 입력 스트림 시작
stream = p.open(format=FORMAT,
                channels=CHANNELS,
                rate=RATE,
                input=True,
                input_device_index=3,
                frames_per_buffer=CHUNK)

print("Recording...")

try:
    while True:
        frames = []

        # 4초 동안 오디오 데이터 수집
        for _ in range(0, int(RATE / CHUNK * DURATION)):
            data = stream.read(CHUNK)
            frames.append(np.frombuffer(data, dtype=np.int16))

        # numpy 배열로 변환
        audio_data = np.hstack(frames)

        # 특징 추출
        features = extraction(audio_data, RATE)
        features = features.reshape(1, -1)  # 모델 입력을 위해 2차원으로 변환

        # 장르 예측
        prediction = model.predict(features)
        predicted_genre = prediction[0]

        genre = class_names[predicted_genre]
        print(f"Predicted Genre: {genre}")

except KeyboardInterrupt:
    print("Recording stopped")

# 스트림 종료
stream.stop_stream()
stream.close()
p.terminate()
