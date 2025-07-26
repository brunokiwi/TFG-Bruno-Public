package com.casa.iot.backend.service;

import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class LightService {

    private final RoomRepository roomRepository;
    private final MqttGateway mqttGateway;

    public LightService(RoomRepository repo, MqttGateway gateway) {
        this.roomRepository = repo;
        this.mqttGateway = gateway;
    }

    public void sendLightCommand(String roomName, boolean lightOn) {
        String topic = roomName + "/light/command/" + (lightOn ? "ON" : "OFF");
        mqttGateway.sendToMqtt("", topic);
    }

    // una vez recibimos respuesta positiva, actualizamos BD
    public void handleConfirmation(String room, String payload) {
        try {
            // parsing {"status":"SUCCESS","state":"ON"}
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String status = json.get("status").getAsString();
            String state = json.get("state").getAsString();
            
            if ("SUCCESS".equals(status)) {
                // bd stuf
                Room roomEntity = roomRepository.findById(room).orElse(new Room(room));
                roomEntity.setLightOn("ON".equals(state));
                roomRepository.save(roomEntity);
                
                System.out.println("Luz actualizada en BD: " + room + " -> " + state);
            } else {
                System.err.println("Error del dispositivo IoT en " + room + ": " + 
                                 json.get("error").getAsString());
            }
        } catch (Exception e) {
            System.err.println("Error al procesar confirmación: " + e.getMessage());
        }
    }
}
