from flask import Flask, jsonify, request
import requests
import os

app = Flask(__name__)

server_ip = "34.45.175.47"  #server ip는 고정됨.
base_directory = '/home/jinhyeok/models'

# server에서 모델 업데이트 완료 시 새로운 모델 다운로드
@app.route('<user_id>/updateModel')
def update_model(user_id):
    url = f"http://{server_ip}:5000/{user_id}/downloadModel"
    user_directory = '/home/jinhyeok/models'
    if not os.path.exists(user_directory):
        os.makedirs(user_directory)
    model_file_path = os.path.join(user_directory, 'model.pkl')
    new_model_file_path = os.path.join(user_directory, 'new_model.pkl')

    response = requests.get(url)
    if response.status_code == 200:
        with open(new_model_file_path, 'wb') as f:
            f.write(response.content)
        os.replace(new_model_file_path, model_file_path)
        return jsonify({'status': 'success', 'message': 'Model updated successfully!'}), 200
    else:
        return jsonify({'status': 'fail', 'message': 'Failed to download model'}), 400


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
