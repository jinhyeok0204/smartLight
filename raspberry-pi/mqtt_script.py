import paho.mqtt.client as mqtt


def hello

def on_connect(client, userdata, flags, rc):

def on_message(client, userdata, msg):
client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

client.connect("")

