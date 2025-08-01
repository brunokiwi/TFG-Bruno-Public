#include <WiFi.h>
#include <PubSubClient.h>
#include <SPI.h>
#include <MFRC522.h>
#include <ArduinoJson.h>
#include <WiFiManager.h> 
#include <Preferences.h>

// MQTT config variables (no const, porque se configuran en runtime)
char mqtt_server[40] = "192.168.1.1";
char mqtt_port[6] = "1883";
char mqtt_client_id[32] = "ESP32_IoT_Device";

Preferences preferences;

// Habitaciones (nombres configurables)
char habitacion1[16] = "salon";
char habitacion2[16] = "cuarto";

// Pines del hardware
const int LED_HAB1 = 2;    // amarillo
const int LED_HAB2 = 4;    // rojo
const int BUZZER_HAB2 = 32; // no conectado
const int PIR_HAB1 = 34;
const int PIR_HAB2 = 35;  // no conectado

// MFRC522
#define SS_PIN 5
#define RST_PIN 22
MFRC522 mfrc522(SS_PIN, RST_PIN);

// Estado de dispositivos
bool hab1LightState = false;
bool hab2LightState = false;
bool hab1AlarmState = false;
bool hab2AlarmState = false;

// Movimiento
bool lastPirhab1State = false;
bool lastPirhab2State = false;
unsigned long lastPirhab1Time = 0;
unsigned long lastPirhab2Time = 0;
const unsigned long PIR_DEBOUNCE_TIME = 2000;

// RFID
bool rfidActive = false;
unsigned long rfidStartTime = 0;
const unsigned long rfidTimeout = 30000; // 30 segundos

WiFiClient espClient;
PubSubClient client(espClient);

// MQTT topics
const char* mqtt_rfid_command_topic = "rfid/command";
const char* mqtt_rfid_result_topic = "rfid/register"; 
const char* mqtt_rfid_event_topic = "rfid/event";    

WiFiManager wm;
WiFiManagerParameter custom_mqtt_server("server", "MQTT Broker", mqtt_server, 40);
WiFiManagerParameter custom_mqtt_port("port", "MQTT Port", mqtt_port, 6);
WiFiManagerParameter custom_mqtt_client("client", "MQTT Client ID", mqtt_client_id, 32);

void setup() {
  Serial.begin(115200);
  SPI.begin();
  mfrc522.PCD_Init();

  pinMode(LED_HAB1, OUTPUT);
  pinMode(LED_HAB2, OUTPUT);
  pinMode(BUZZER_HAB2, OUTPUT);
  pinMode(PIR_HAB1, INPUT);
  pinMode(PIR_HAB2, INPUT);

  digitalWrite(LED_HAB1, LOW);
  digitalWrite(LED_HAB2, LOW);
  digitalWrite(BUZZER_HAB2, LOW);

  preferences.begin("mqtt", false);
  if (preferences.isKey("server")) {
    preferences.getString("server", mqtt_server, sizeof(mqtt_server));
    preferences.getString("port", mqtt_port, sizeof(mqtt_port));
    preferences.getString("client", mqtt_client_id, sizeof(mqtt_client_id));
    Serial.println("Parámetros MQTT cargados de la flash.");
  }

  wm.addParameter(&custom_mqtt_server);
  wm.addParameter(&custom_mqtt_port);
  wm.addParameter(&custom_mqtt_client);

  startWifiAndMqttSetup();

  Serial.println("ESP32 IoT Device iniciado");
  Serial.print("Habitaciones: ");
  Serial.print(habitacion1);
  Serial.print(", ");
  Serial.println(habitacion2);
}

void startWifiAndMqttSetup() {
  bool conectado = false;
  while (!conectado) {
    if (!wm.autoConnect("CasaIoT-Setup")) {
      Serial.println("Fallo al conectar y timeout");
      ESP.restart();
      delay(1000);
    }

    // no a flash todavia
    strcpy(mqtt_server, custom_mqtt_server.getValue());
    strcpy(mqtt_port, custom_mqtt_port.getValue());
    strcpy(mqtt_client_id, custom_mqtt_client.getValue());

    Serial.println("WiFi conectado");
    Serial.print("IP: "); Serial.println(WiFi.localIP());
    Serial.print("MQTT Broker: "); Serial.println(mqtt_server);
    Serial.print("MQTT Port: "); Serial.println(mqtt_port);
    Serial.print("MQTT Client: "); Serial.println(mqtt_client_id);

    // MQTT
    client.setServer(mqtt_server, atoi(mqtt_port));
    client.setCallback(onMqttMessage);

    if (reconnectMqttWithTimeout(15000)) { // 15 secs de timeout
      preferences.putString("server", mqtt_server);
      preferences.putString("port", mqtt_port);
      preferences.putString("client", mqtt_client_id);
      conectado = true;
    } else {
      Serial.println("No se pudo conectar a MQTT. Reiniciando configuración WiFiManager...");
      wm.resetSettings(); // borrar config y reintentar
      delay(1000);
    }
  }
}

bool reconnectMqttWithTimeout(unsigned long timeoutMs) {
  unsigned long startAttempt = millis();
  while (!client.connected() && (millis() - startAttempt < timeoutMs)) {
    Serial.print("Conectando a MQTT...");
    if (client.connect(mqtt_client_id)) {
      Serial.println("conectado");
      client.subscribe((String(habitacion1) + "/lig/command").c_str());
      client.subscribe((String(habitacion1) + "/mov/command").c_str());
      client.subscribe((String(habitacion2) + "/lig/command").c_str());
      client.subscribe((String(habitacion2) + "/mov/command").c_str());
      client.subscribe((String(habitacion2) + "/sou/command").c_str());
      client.subscribe("REMOVE");
      client.subscribe(mqtt_rfid_command_topic);
      Serial.println("Suscrito a tópicos MQTT");
      return true;
    } else {
      Serial.print("falló, rc=");
      Serial.print(client.state());
      Serial.println(" reintentando en 5 segundos");
      delay(5000);
    }
  }
  return client.connected();
}

void loop() {
  if (!client.connected()) {
    Serial.println("MQTT desconectado. Reiniciando configuración WiFiManager...");
    wm.resetSettings(); // Borra la configuración WiFi y MQTT
    delay(1000);
    ESP.restart(); // Reinicia el ESP para volver a setup()
  }
  client.loop();

  // Sensores de movimiento, si no hay encendido nos ahorramos
  if (hab1AlarmState || hab2AlarmState){
    checkMovementSensors();
  }

  // RFID
  handleRfidLogic();

  delay(100);
}

void handleRfidLogic() {
  if (rfidActive) {
    if (millis() - rfidStartTime > rfidTimeout) {
      // timeout
      StaticJsonDocument<128> doc;
      doc["event"] = "RFID_REGISTER_CANCEL";
      char buffer[128];
      serializeJson(doc, buffer);
      client.publish(mqtt_rfid_result_topic, buffer);
      rfidActive = false;
      Serial.println("Timeout RFID registro");
    } else if (mfrc522.PICC_IsNewCardPresent() && mfrc522.PICC_ReadCardSerial()) {
      String uid = "";
      for (byte i = 0; i < mfrc522.uid.size; i++) {
        char hex[3];
        sprintf(hex, "%02X", mfrc522.uid.uidByte[i]);
        uid += hex;
      }
      StaticJsonDocument<128> doc;
      doc["event"] = "RFID_REGISTER";
      doc["cardId"] = uid;
      char buffer[128];
      serializeJson(doc, buffer);
      client.publish(mqtt_rfid_result_topic, buffer);
      Serial.print("Tarjeta registrada: "); Serial.println(uid);
      rfidActive = false;
      mfrc522.PICC_HaltA();
    }
  } else {
    // Detección normal de RFID (no registro)
    if (mfrc522.PICC_IsNewCardPresent() && mfrc522.PICC_ReadCardSerial()) {
      String uid = "";
      for (byte i = 0; i < mfrc522.uid.size; i++) {
        char hex[3];
        sprintf(hex, "%02X", mfrc522.uid.uidByte[i]);
        uid += hex;
      }
      // Publicar UID por MQTT (detección normal)
      StaticJsonDocument<128> doc;
      doc["event"] = "RFID_DETECTED";
      doc["cardId"] = uid;
      char buffer[128];
      serializeJson(doc, buffer);
      client.publish(mqtt_rfid_event_topic, buffer);
      Serial.print("Tarjeta detectada (normal): "); Serial.println(uid);
      mfrc522.PICC_HaltA();
    }
  }
}

void reconnectMqtt() {
  while (!client.connected()) {
    Serial.print("Conectando a MQTT...");
    if (client.connect(mqtt_client_id)) {
      Serial.println("conectado");
      client.subscribe((String(habitacion1) + "/lig/command").c_str());
      client.subscribe((String(habitacion1) + "/mov/command").c_str());
      client.subscribe((String(habitacion2) + "/lig/command").c_str());
      client.subscribe((String(habitacion2) + "/mov/command").c_str());
      client.subscribe((String(habitacion2) + "/sou/command").c_str());
      client.subscribe("REMOVE");
      client.subscribe(mqtt_rfid_command_topic);
      Serial.println("Suscrito a tópicos MQTT");
    } else {
      Serial.print("falló, rc=");
      Serial.print(client.state());
      Serial.println(" reintentando en 5 segundos");
      delay(5000);
    }
  }
}

void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  String message = "";
  for (unsigned int i = 0; i < length; i++) message += (char)payload[i];

  Serial.print("Mensaje recibido [");
  Serial.print(topic);
  Serial.print("]: ");
  Serial.println(message);

  String topicStr = String(topic);

  if (topicStr == "REMOVE") {
    handleRoomRemoval(message);
  } else if (topicStr.endsWith("/lig/command")) {
    handleLightCommand(topicStr, message);
  } else if (topicStr.endsWith("/mov/command")) {
    handleAlarmCommand(topicStr, message);
  } else if (topicStr.endsWith("/sou/command")) {
    handleSoundCommand(topicStr, message);
  } else if (topicStr == mqtt_rfid_command_topic) {
    if (message.indexOf("START") != -1) {
      rfidActive = true;
      rfidStartTime = millis();
      Serial.println("Lector RFID ACTIVADO (registro)");
    } else if (message.indexOf("CANCEL") != -1) {
      rfidActive = false;
      Serial.println("Lector RFID CANCELADO");
      StaticJsonDocument<128> doc;
      doc["event"] = "RFID_REGISTER_CANCEL";
      char buffer[128];
      serializeJson(doc, buffer);
      client.publish(mqtt_rfid_result_topic, buffer);
    }
  }
}

void handleLightCommand(String topic, String payload) {
  String room = topic.substring(0, topic.indexOf("/"));
  DynamicJsonDocument doc(200);
  deserializeJson(doc, payload);

  String command = doc["command"];
  String state = doc["state"];

  if (command == "SET_LIGHT") {
    bool newState = (state == "ON");
    bool success = false;

    if (room == String(habitacion1)) {
      digitalWrite(LED_HAB1, newState ? HIGH : LOW);
      hab1LightState = newState;
      success = true;
    } else if (room == String(habitacion2)) {
      digitalWrite(LED_HAB2, newState ? HIGH : LOW);
      hab2LightState = newState;
      success = true;
    }

    sendLightConfirmation(room, newState, success);

    Serial.print("Luz ");
    Serial.print(room);
    Serial.print(" cambiada a: ");
    Serial.println(newState ? "ON" : "OFF");
  }
}

void handleAlarmCommand(String topic, String payload) {
  String room = topic.substring(0, topic.indexOf("/"));
  DynamicJsonDocument doc(200);
  deserializeJson(doc, payload);

  String command = doc["command"];
  String state = doc["state"];

  if (command == "SET_ALARM") {
    bool newState = (state == "ON");
    bool success = false;

    if (room == String(habitacion1)) {
      hab1AlarmState = newState;
      success = true;
    } else if (room == String(habitacion2)) {
      hab2AlarmState = newState;
      success = true;
    }

    sendAlarmConfirmation(room, newState, success);

    Serial.print("Alarma ");
    Serial.print(room);
    Serial.print(" cambiada a: ");
    Serial.println(newState ? "ON" : "OFF");
  }
}

void handleSoundCommand(String topic, String payload) {
  String room = topic.substring(0, topic.indexOf("/"));
  if (room != String(habitacion2)) return;

  DynamicJsonDocument doc(200);
  deserializeJson(doc, payload);

  String command = doc["command"];
  if (command == "PLAY_SOUND") {
    String sound = doc["sound"];
    int volume = doc["volume"];
    digitalWrite(BUZZER_HAB2, HIGH);
    delay(3000);
    digitalWrite(BUZZER_HAB2, LOW);
    sendSoundConfirmation(room, true);
    Serial.print("Sonido reproducido en ");
    Serial.print(room);
    Serial.print(": ");
    Serial.println(sound);
  }
}

void handleRoomRemoval(String roomName) {
  Serial.print("Habitación eliminada del sistema: ");
  Serial.println(roomName);

  if (roomName == String(habitacion1)) {
    digitalWrite(LED_HAB1, LOW);
    hab1LightState = false;
    hab1AlarmState = false;
  } else if (roomName == String(habitacion2)) {
    digitalWrite(LED_HAB2, LOW);
    digitalWrite(BUZZER_HAB2, LOW);
    hab2LightState = false;
    hab2AlarmState = false;
  }
}

void checkMovementSensors() {
  unsigned long currentTime = millis();

  bool currentPirhab1 = digitalRead(PIR_HAB1);
  if (currentPirhab1 && !lastPirhab1State &&
      (currentTime - lastPirhab1Time > PIR_DEBOUNCE_TIME) &&
      hab1AlarmState) {

    sendMovementDetected(String(habitacion1));
    lastPirhab1Time = currentTime;
  }
  lastPirhab1State = currentPirhab1;

  bool currentPirhab2 = digitalRead(PIR_HAB2);
  if (currentPirhab2 && !lastPirhab2State &&
      (currentTime - lastPirhab2Time > PIR_DEBOUNCE_TIME) &&
      hab2AlarmState) {

    sendMovementDetected(String(habitacion2));
    lastPirhab2Time = currentTime;
  }
  lastPirhab2State = currentPirhab2;
}

void sendLightConfirmation(String room, bool state, bool success) {
  DynamicJsonDocument doc(200);
  if (success) {
    doc["status"] = "SUCCESS";
    doc["state"] = state ? "ON" : "OFF";
  } else {
    doc["status"] = "ERROR";
    doc["error"] = "DEVICE_OFFLINE";
  }
  String payload;
  serializeJson(doc, payload);
  String topic = room + "/lig/confirmation";
  client.publish(topic.c_str(), payload.c_str());
}

void sendAlarmConfirmation(String room, bool state, bool success) {
  DynamicJsonDocument doc(200);
  if (success) {
    doc["status"] = "SUCCESS";
    doc["state"] = state ? "ON" : "OFF";
  } else {
    doc["status"] = "ERROR";
    doc["error"] = "SENSOR_MALFUNCTION";
  }
  String payload;
  serializeJson(doc, payload);
  String topic = room + "/mov/confirmation";
  client.publish(topic.c_str(), payload.c_str());
}

void sendSoundConfirmation(String room, bool success) {
  DynamicJsonDocument doc(200);
  if (success) {
    doc["status"] = "SUCCESS";
    doc["action"] = "SOUND_PLAYED";
  } else {
    doc["status"] = "ERROR";
    doc["error"] = "BUZZER_MALFUNCTION";
  }
  String payload;
  serializeJson(doc, payload);
  String topic = room + "/sou/confirmation";
  client.publish(topic.c_str(), payload.c_str());
}

void sendMovementDetected(String room) {
  DynamicJsonDocument doc(200);
  doc["event"] = "MOVEMENT_DETECTED";
  doc["timestamp"] = "2024-01-20T15:30:00";
  String payload;
  serializeJson(doc, payload);
  String topic = room + "/mov/event";
  client.publish(topic.c_str(), payload.c_str());
  Serial.print("Movimiento detectado en: ");
  Serial.println(room);
}