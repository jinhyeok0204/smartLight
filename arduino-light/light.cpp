#include <ESP8266WiFi.h>
#include <PubSubClient.h>

const char* ssid = "your_SSID";
const char* password = "your_PASSWORD";
const char* mqtt_server = "your_raspberry_pi_ip";

WiFiClient espClient;
PubSubClient client(espClient);

const int lightPin = D1; // 조명이 연결된 핀
bool lightState = false;

void setup_wifi() {
  delay(10);
  Serial.begin(115200);
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connected");
}

void callback(char* topic, byte* payload, unsigned int length) {
  String message;
  for (unsigned int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  
  if (String(topic) == "home/light3") {
    if (message == "ON") {
      lightState = true;
      digitalWrite(lightPin, HIGH);
    } else if (message == "OFF") {
      lightState = false;
      digitalWrite(lightPin, LOW);
    }
  }
}

void reconnect() {
  while (!client.connected()) {
    if (client.connect("ArduinoClient3")) {
      client.subscribe("home/light3");
    } else {
      delay(5000);
    }
  }
}

void setup() {
  pinMode(lightPin, OUTPUT);
  digitalWrite(lightPin, LOW);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
}
