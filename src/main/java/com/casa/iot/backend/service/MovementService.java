package com.casa.iot.backend.service;

import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class MovementService {
    private final MqttGateway mqttGateway;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;
    private final EventLogService eventLogService;

    public MovementService(RoomRepository repo, MqttGateway gateway, 
                          NotificationService notificationService, EventLogService eventLogService) {
        this.roomRepository = repo;
        this.mqttGateway = gateway;
        this.notificationService = notificationService;
        this.eventLogService = eventLogService;
    }

    public void sendAlarmCommand(String roomName, boolean alarmOn) {
        String topic = roomName + "/alarm/command/" + (alarmOn ? "ON" : "OFF");
        mqttGateway.sendToMqtt(topic);
    }

    public void handleConfirmation(String room, String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String status = json.get("status").getAsString();
            String state = json.get("state").getAsString();
            
            if ("SUCCESS".equals(status)) {
                Room roomEntity = roomRepository.findById(room).orElse(new Room(room));
                roomEntity.setDetectOn("ON".equals(state));
                roomRepository.save(roomEntity);
                
                // logging
                String action = "ON".equals(state) ? "SENSOR_ON" : "SENSOR_OFF";
                String details = String.format("{\"state\":\"%s\",\"source\":\"DEVICE_CONFIRMATION\",\"timestamp\":\"%s\"}", 
                                              state, java.time.LocalDateTime.now());
                eventLogService.logSystemAction(action, room, details, "DEVICE_CONFIRMATION");
                
                System.out.println("Sensor actualizado en BD: " + room + " -> " + state);
            } else {
                System.err.println("Error del sensor IoT en " + room + ": " + 
                                 json.get("error").getAsString());
            }
        } catch (Exception e) {
            System.err.println("Error al procesar confirmacion del sensor: " + e.getMessage());
        }
    }

    public void handle(String room, String payload) {
        try {
            System.out.println("Movimiento recibido del sensor en " + room + ": " + payload);
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String event = json.get("event").getAsString();
            
            // notificacion
            //TODO: Ejecutar acciones automáticas (encender luz, etc.)
            if ("MOVEMENT_DETECTED".equals(event)) {
                Room roomEntity = roomRepository.findById(room).orElse(null);
                
                if (roomEntity != null && roomEntity.isDetectOn()) {
                    // logging
                    String details = String.format("{\"timestamp\":\"%s\",\"sensorType\":\"PIR\",\"sensorActive\":true}", 
                                                  json.has("timestamp") ? json.get("timestamp").getAsString() : java.time.LocalDateTime.now());
                    eventLogService.logMovementDetected(room, details);
                    
                    System.out.println("Movimiento detectado en " + room + " - Enviando notificacion");
                    notificationService.sendMovementAlert(room);
                } else {
                    // logging
                    String details = String.format("{\"timestamp\":\"%s\",\"sensorType\":\"PIR\",\"sensorActive\":false,\"reason\":\"SENSOR_DISABLED\"}", 
                                                  json.has("timestamp") ? json.get("timestamp").getAsString() : java.time.LocalDateTime.now());
                    eventLogService.logSystemAction("MOVEMENT_IGNORED", room, details, "MOVEMENT_SENSOR");
                    
                    System.out.println("Movimiento detectado en " + room + " - Sensor desactivado, no se envia notificacion");
                }
                
            } else if ("ALARM_CHANGED".equals(event)) {
                String state = json.get("state").getAsString();
                Room roomEntity = roomRepository.findById(room).orElse(new Room(room));
                roomEntity.setDetectOn("ON".equals(state));
                roomRepository.save(roomEntity);
                
                // logging
                String action = "ON".equals(state) ? "SENSOR_ON" : "SENSOR_OFF";
                String details = String.format("{\"state\":\"%s\",\"source\":\"MANUAL\",\"timestamp\":\"%s\"}", 
                                              state, json.has("timestamp") ? json.get("timestamp").getAsString() : java.time.LocalDateTime.now());
                eventLogService.logSystemAction(action, room, details, "MANUAL");
                
                System.out.println("Sensor cambiado manualmente: " + room + " -> " + state);
            }
        } catch (Exception e) {
            System.err.println("Error al procesar evento del sensor: " + e.getMessage());
        }
    }
}
