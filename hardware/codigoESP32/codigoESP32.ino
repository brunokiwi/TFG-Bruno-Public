#include <WiFi.h>
#include <PubSubClient.h>
#include <SPI.h>
#include <MFRC522.h>
#include <ArduinoJson.h>
#include <WiFiManager.h> 
#include <Preferences.h>

// MQTT config variables (no const, porque se configuran en runtime)
char mqtt_server[40] = "192.168.1.57";
char mqtt_port[6] = "1883";
char mqtt_client_id[32] = "ESP32_IoT_Device";

Preferences preferences;

// Pines del hardware
const int LED_SALON = 2;    // amarillo
const int LED_CUARTO = 4;   // rojo
const int BUZZER_CUARTO = 32; // no conectado
const int PIR_SALON = 34;
const int PIR_CUARTO = 15;  // no conectado

// MFRC522
#define SS_PIN 5
#define RST_PIN 22
MFRC522 mfrc522(SS_PIN, RST_PIN);

// Estado de dispositivos
bool salonLightState = false;
bool cuartoLightState = false;
bool salonAlarmState = false;
bool cuartoAlarmState = false;

// Movimiento
bool lastPirSalonState = false;
bool lastPirCuartoState = false;
unsigned long lastPirSalonTime = 0;
unsigned long lastPirCuartoTime = 0;
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

void setup() {
  Serial.begin(115200);
  SPI.begin();
  mfrc522.PCD_Init();

  // Pines
  pinMode(LED_SALON, OUTPUT);
  pinMode(LED_CUARTO, OUTPUT);
  pinMode(BUZZER_CUARTO, OUTPUT);
  pinMode(PIR_SALON, INPUT);
  pinMode(PIR_CUARTO, INPUT);

  digitalWrite(LED_SALON, LOW);
  digitalWrite(LED_CUARTO, LOW);
  digitalWrite(BUZZER_CUARTO, LOW);

  // cargar config guardada
  preferences.begin("mqtt", false);
  if (preferences.isKey("server")) {
    preferences.getString("server", mqtt_server, sizeof(mqtt_server));
    preferences.getString("port", mqtt_port, sizeof(mqtt_port));
    preferences.getString("client", mqtt_client_id, sizeof(mqtt_client_id));
    Serial.println("Parámetros MQTT cargados de la flash.");
  }

  // wifimanager setup
  WiFiManager wm;
  // wm.resetSettings(); <- descomenta para probar flash
  // delay(1000);
  WiFiManagerParameter custom_mqtt_server("server", "MQTT Broker", mqtt_server, 40);
  WiFiManagerParameter custom_mqtt_port("port", "MQTT Port", mqtt_port, 6);
  WiFiManagerParameter custom_mqtt_client("client", "MQTT Client ID", mqtt_client_id, 32);

  wm.addParameter(&custom_mqtt_server);
  wm.addParameter(&custom_mqtt_port);
  wm.addParameter(&custom_mqtt_client);

  // Portal cautivo para configurar WiFi y MQTT si es necesario
  if (!wm.autoConnect("CasaIoT-Setup")) {
    Serial.println("Fallo al conectar y timeout");
    ESP.restart();
    delay(1000);
  }

  // Guardar los parámetros introducidos
  strcpy(mqtt_server, custom_mqtt_server.getValue());
  strcpy(mqtt_port, custom_mqtt_port.getValue());
  strcpy(mqtt_client_id, custom_mqtt_client.getValue());

  // Guardar en la flash
  preferences.putString("server", mqtt_server);
  preferences.putString("port", mqtt_port);
  preferences.putString("client", mqtt_client_id);

  Serial.println("WiFi conectado");
  Serial.print("IP: "); Serial.println(WiFi.localIP());
  Serial.print("MQTT Broker: "); Serial.println(mqtt_server);
  Serial.print("MQTT Port: "); Serial.println(mqtt_port);
  Serial.print("MQTT Client: "); Serial.println(mqtt_client_id);

  // MQTT
  client.setServer(mqtt_server, atoi(mqtt_port));
  client.setCallback(onMqttMessage);

  reconnectMqtt();

  Serial.println("ESP32 IoT Device iniciado");
  Serial.println("Habitaciones: salon, cuarto");
}

void loop() {
  if (!client.connected()) reconnectMqtt();
  client.loop();

  // Sensores de movimiento, si no hay encendido nos ahorramos
  if (salonAlarmState || cuartoAlarmState){
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
      // Suscribirse a tópicos de comandos
      client.subscribe("salon/lig/command");
      client.subscribe("salon/mov/command");
      client.subscribe("cuarto/lig/command");
      client.subscribe("cuarto/mov/command");
      client.subscribe("cuarto/sou/command");
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

    // Aqui se pueden añadir mas habitaciones
    if (room == "salon") {
      digitalWrite(LED_SALON, newState ? HIGH : LOW);
      salonLightState = newState;
      success = true;
    } else if (room == "cuarto") {
      digitalWrite(LED_CUARTO, newState ? HIGH : LOW);
      cuartoLightState = newState;
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

    // Aqui se pueden añadir mas habitaciones
    if (room == "salon") {
      salonAlarmState = newState;
      success = true;
    } else if (room == "cuarto") {
      cuartoAlarmState = newState;
      success = true;
    }

    sendAlarmConfirmation(room, newState, success);

    Serial.print("Alarma ");
    Serial.print(room);
    Serial.print(" cambiada a: ");
    Serial.println(newState ? "ON" : "OFF");
  }
}

// TODO
void handleSoundCommand(String topic, String payload) {
  String room = topic.substring(0, topic.indexOf("/"));
  if (room != "cuarto") return;

  DynamicJsonDocument doc(200);
  deserializeJson(doc, payload);

  String command = doc["command"];
  if (command == "PLAY_SOUND") {
    String sound = doc["sound"];
    int volume = doc["volume"];
    digitalWrite(BUZZER_CUARTO, HIGH);
    delay(3000);
    digitalWrite(BUZZER_CUARTO, LOW);
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

  if (roomName == "salon") {
    digitalWrite(LED_SALON, LOW);
    salonLightState = false;
    salonAlarmState = false;
  } else if (roomName == "cuarto") {
    digitalWrite(LED_CUARTO, LOW);
    digitalWrite(BUZZER_CUARTO, LOW);
    cuartoLightState = false;
    cuartoAlarmState = false;
  }
}

void checkMovementSensors() {
  unsigned long currentTime = millis();

  bool currentPirSalon = digitalRead(PIR_SALON);
  if (currentPirSalon && !lastPirSalonState &&
      (currentTime - lastPirSalonTime > PIR_DEBOUNCE_TIME) &&
      salonAlarmState) {

    sendMovementDetected("salon");
    lastPirSalonTime = currentTime;
  }
  lastPirSalonState = currentPirSalon;

  bool currentPirCuarto = digitalRead(PIR_CUARTO);
  if (currentPirCuarto && !lastPirCuartoState &&
      (currentTime - lastPirCuartoTime > PIR_DEBOUNCE_TIME) &&
      cuartoAlarmState) {

    sendMovementDetected("cuarto");
    lastPirCuartoTime = currentTime;
  }
  lastPirCuartoState = currentPirCuarto;
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