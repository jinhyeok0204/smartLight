import paho.mqtt.client as mqtt

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))
    client.subscribe("home/#") # 모든 조명 관련 토픽 구독


def on_message(client, userdata, msg):
    message = msg.payload.decode()
    if msg.topic in light_states: # light_states 얻어오는 경우
        light_states



client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

client.connect("")

