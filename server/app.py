from flask import Flask, send_from_directory, request, jsonify
import requests
import json
import os

# server 내부 동작 코드

app = Flask(__name__)

## 1. 라즈베리파이에서 피드백 파일 얻기 -> 특정 유저의 모델을 학습시켜야 함.
@app.route('<user_id>/uploadFeedback', methods=['POST'])
def get_feedback_from_pi(user_id: str):

    # path : userId/feedback.json
    feedback_file_path = os.path.join(user_id, 'feedback.json') # feedback file format고민하기
    file = request.files['feedback']

    file.save(feedback_file_path)
    refine_model(user_id)
    return jsonify({'status': 'success', 'message': 'feedback data uploaded successfully'}), 200

## 2. 얻은 피드백 파일에 따라 모델 재학습
def refine_model(user_id):
    # refine code ~~~~~~ 별도 파일 분리?
    #TODO(모델 재학습 코드 작성)
    notify_raspberry_pi()
    pass


## 3. 모델 학습 완료 시, 라즈베리파이가 모델을 다운로드하여 기존 모델 대체할 수 있도록 하기

# 3-1. 모델 재학습 완료 시, 라즈베리파이에 시그널 보냄
def notify_raspberry_pi():
    pi_ip = 'ip of raspberry pi' # 수정 필요  user_id마다 다른 Ip가짐 ->
    url = f'http://{pi_ip}:5000/updateModel'  # raspberry-pi에서 라우팅 처리
    try:
        response = requests.get(url)
        if response.status_code == 200:
            print('Raspberry PI notified Successfully')
        else:
            print('Failed to notify Raspberry Pi')
    except requests.exceptions.RequestException as e:
        print(f'Error Occured: {e}')


# 3-2. 시그널을 받은 라즈베리파이가 get 요청을 보내게 됨.
@app.route('<user_id>/downloadModel', methods=['GET'])
def download_model(user_id: str):
    directory = f'{user_id}/models'  # userID에 따라 모델 분류
    return send_from_directory(directory, 'model.pkl')


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)