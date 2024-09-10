import sqlite3

from flask import Flask, send_from_directory, request, jsonify, g
import requests
import json
import os
from werkzeug.security import generate_password_hash, check_password_hash
# server 내부 동작 코드

app = Flask(__name__)
DATABASE = 'users.db'


## 1. 라즈베리파이에서 피드백 파일 얻기 -> 특정 유저의 모델을 학습시켜야 함.
# 핸드폰에서 바로 해당 서버로 피드백 얻어오고, 특정 개수 이상이 되면 재학습 시키기..
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
        print(f'Error Occurred: {e}')


# 3-2. 시그널을 받은 라즈베리파이가 get 요청을 보내게 됨.
@app.route('<user_id>/downloadModel', methods=['GET'])
def download_model(user_id: str):
    directory = f'{user_id}/models'  # userID에 따라 모델 분류
    return send_from_directory(directory, 'model.pkl')


## 4. 핸드폰에서 회원가입 시 Database에 등록 / 로그인 시 Database에서 확인

# 회원가입 처리
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    hashed_password = generate_password_hash(password, method='pbkdf:sha256', salt_length=16)
    db = get_db()
    db.execute('INSERT INTO users (username, password) VALUES (?, ?)', (username, hashed_password))
    db.commit()
    return jsonify({'status': 'User registered successfully'}), 200

@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = get_db()
    user = db.execute('SELECT * FROM users WHERE username = ?', (username,)).fetchone()
    if user and check_password_hash(user[2], password):
        return jsonify({'status': 'Login successful', 'user_id': user[0]})
    else:
        return jsonify({'status': 'Invalid UserID or Password'}), 401


def get_db():
    db = getattr(g, '_database', None)
    if db is None:
        db = g._database = sqlite3.connect(DATABASE)
    return db


def close_connection(exception):
    db = getattr(g, '_database', None)
    if db is not None:
        db.close()


def init_db():
    with app.app_context():
        db = get_db()
        with app.open_instance_resource('schema.sql') as f:
            db.executescript(f.read().decode('utf8'))


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)