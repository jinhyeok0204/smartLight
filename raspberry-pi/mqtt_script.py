import paho.mqtt.client as mqtt
import logging

# MQTT 브로커 설정 (라즈베리파이 자체가 브로커)
mqtt_broker = 'localhost'
mqtt_port = 1883
mqtt_client_id = "RaspberryPiClient"

# 조명 상태 dictionary
light_status = {
    '1' : 'OFF',
    '2' : 'OFF',
    '3' : 'OFF',
}

# MQTT 연결 시 호출되는 call back function
def on_connect(client, userdata, flags, rc):
    logging.info(f"Connected with result code {rc}")
    for i in range(1, 4):
        client.subscribe(f"home/light{i}/status")
        client.subscribe(f"home/light{i}/toggle")

# MQTT 메시지 수신 시 호출되는 call back function
def on_message(client, userdata, msg):
    logging.info(f"Recived Message: {msg.topic} -> {msg.payload.decode()}")
    topic_parts = msg.topic.split('/')
    light_number = int(topic_parts[1][-1])

    if 'toggle' in msg.topic:
        new_status = msg.payload.decode()
        light_status[light_number] = new_status
        # 아두이노에 상태 변경 메시지 전송
        client.publish(f"arduino/light{light_number}/toggle", new_status)
    elif 'status' in msg.topic:
        # 아두이노에 상태 요청 메시지 전송
        client.publish(f"arduino/light{light_number}/status", "get")

def on_arduino_message(client, userdata, msg):
    logging.info(f"Recived Message: {msg.topic} -> {msg.payload.decode()}")
    topic_parts = msg.topic.split('/')
    light_number = topic_parts[1][-1]

    client.publish(f"home/light{light_number}/status", msg.payload.decode())



client = mqtt.Client(mqtt_client_id)
client.on_connect = on_connect
client.on_message = on_message

# arduino/ 로 시작하고 그 뒤에 어떤 것이 오더라도 상관없음.
# 아두이노로부터 받은 메시지를 처리하는 call back function
client.message_callback_add("arduino/+", on_arduino_message)

# MQTT 브로커 연결
client.connect(mqtt_broker, mqtt_port, 10)

# MQTT 루프 시작
client.loop_forever()

