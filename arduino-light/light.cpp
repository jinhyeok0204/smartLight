#include <ESP8266WiFi.h>
#include <PubSubClient.h>

// WiFi 설정
const char* ssid = "your_SSID";
const char* password = "your_PASSWORD";

// MQTT 브로커 설정 (라즈베리 파이 IP 주소를 사용)
const char* mqtt_server = "raspberry_pi_ip_address";
const int mqtt_port = 1883;
const char* mqtt_client_id = "ArduinoClient";

// 조명 핀 설정
const int lightPins[] = {5, 4, 0};  // 변경: 아두이노 나노 핀 번호에 맞게 설정
int lightStatus[] = {LOW, LOW, LOW};
const int lightCount = 3;

WiFiClient espClient;
PubSubClient client(espClient);

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
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void handleToggle(int lightIndex) {
  lightStatus[lightIndex] = !lightStatus[lightIndex];
  digitalWrite(lightPins[lightIndex], lightStatus[lightIndex]);
  client.publish(("arduino/light" + String(lightIndex + 1) + "/status").c_str(), lightStatus[lightIndex] ? "ON" : "OFF");
}

void handleStatus(int lightIndex) {
  client.publish(("arduino/light" + String(lightIndex + 1) + "/status").c_str(), lightStatus[lightIndex] ? "ON" : "OFF");
}

void callback(char* topic, byte* payload, unsigned int length) {
  String message;
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  Serial.println(message);

  for (int i = 0; i < lightCount; i++) {
    String lightTopic = "arduino/light" + String(i + 1);
    if (String(topic) == lightTopic + "/toggle") {
      handleToggle(i);
    } else if (String(topic) == lightTopic + "/status" && message == "get") {
      handleStatus(i);
    }
  }
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    if (client.connect(mqtt_client_id)) {
      Serial.println("connected");
      for (int i = 1; i <= lightCount; i++) {
        client.subscribe(("arduino/light" + String(i) + "/toggle").c_str());
        client.subscribe(("arduino/light" + String(i) + "/status").c_str());
      }
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}

void setup() {
  for (int i = 0; i < lightCount; i++) {
    pinMode(lightPins[i], OUTPUT);
  }

  setup_wifi();
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
}
