package com.casa.iot.backend.service;

import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;

@Service
public class MovementService {
    private final MqttGateway mqttGateway; // to send messages to iOT
    private final RoomRepository roomRepository;

    public MovementService(RoomRepository repo, MqttGateway gateway) {
        this.roomRepository = repo;
        this.mqttGateway = gateway;
    }

    public void handle(String room, String payload) {
        // TODO recibir movimiento, guardar en bd? y enviar notificación a la app
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }

    public Room updateAlarm(String roomName, boolean state) {
        Room room = roomRepository.findById(roomName).orElse(new Room(roomName));
        room.setDetectOn(state);
        Room updated = roomRepository.save(room);

        // send to iot
        mqttGateway.sendToMqtt(state ? "ON" : "OFF", roomName + "/mov");
        return updated;
    }

    
}
