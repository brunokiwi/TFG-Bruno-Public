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

    public MovementService(RoomRepository repo, MqttGateway gateway, NotificationService notificationService) {
        this.roomRepository = repo;
        this.mqttGateway = gateway;
        this.notificationService = notificationService;
    }

    // SOLO envía comando al sensor - NO actualiza BD
    public void sendAlarmCommand(String roomName, boolean alarmOn) {
        // Enviar comando con formato JSON
        String payload = String.format("{\"command\":\"SET_ALARM\",\"state\":\"%s\"}", 
                                     alarmOn ? "ON" : "OFF");
        mqttGateway.sendToMqtt(payload, roomName + "/mov/command");
    }

    // Procesa confirmación del sensor y ENTONCES actualiza BD
    public void handleConfirmation(String room, String payload) {
        try {
            // Parsear respuesta del sensor: {"status":"SUCCESS","state":"ON"}
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String status = json.get("status").getAsString();
            String state = json.get("state").getAsString();
            
            if ("SUCCESS".equals(status)) {
                // AHORA SÍ actualizamos la base de datos
                Room roomEntity = roomRepository.findById(room).orElse(new Room(room));
                roomEntity.setDetectOn("ON".equals(state));
                roomRepository.save(roomEntity);
                
                System.out.println("Sensor actualizado en BD: " + room + " -> " + state);
            } else {
                System.err.println("Error del sensor IoT en " + room + ": " + 
                                 json.get("error").getAsString());
            }
        } catch (Exception e) {
            System.err.println("Error al procesar confirmacion del sensor: " + e.getMessage());
        }
    }

    // Maneja eventos de detección de movimiento
    public void handle(String room, String payload) {
        try {
            System.out.println("Movimiento recibido del sensor en " + room + ": " + payload);
            // Evento de movimiento: {"event":"MOVEMENT_DETECTED","timestamp":"2024-01-20T15:30:00"}
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String event = json.get("event").getAsString();
            
            // notificacion
            //TODO: Ejecutar acciones automáticas (encender luz, etc.)
            if ("MOVEMENT_DETECTED".equals(event)) {
                Room roomEntity = roomRepository.findById(room).orElse(null);
                
                if (roomEntity != null && roomEntity.isDetectOn()) {
                    System.out.println("Movimiento detectado en " + room + " - Enviando notificacion");
                    notificationService.sendMovementAlert(room);
                } else {
                    System.out.println("Movimiento detectado en " + room + " - Sensor desactivado, no se envia notificacion");
                }
                
            } else if ("ALARM_CHANGED".equals(event)) {
                // Cambio manual del sensor (activado/desactivado físicamente)
                String state = json.get("state").getAsString();
                Room roomEntity = roomRepository.findById(room).orElse(new Room(room));
                roomEntity.setDetectOn("ON".equals(state));
                roomRepository.save(roomEntity);
                
                System.out.println("Sensor cambiado manualmente: " + room + " -> " + state);
            }
        } catch (Exception e) {
            System.err.println("Error al procesar evento del sensor: " + e.getMessage());
        }
    }
}
