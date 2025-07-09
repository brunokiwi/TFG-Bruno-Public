package com.casa.iot.backend.service;

import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;

@Service
public class LightService {

    private final RoomRepository roomRepository;
    private final MqttGateway mqttGateway; // to send messages to iOT

    public LightService(RoomRepository repo, MqttGateway gateway) {
        this.roomRepository = repo;
        this.mqttGateway = gateway;
    }

    public Room updateLight(String name, boolean lightOn) {
        // TODO que se hace primero?
        // update en bd
        Room room = roomRepository.findById(name).orElse(new Room(name));
        room.setLightOn(lightOn);
        Room updated = roomRepository.save(room);

        // send to iot
        mqttGateway.sendToMqtt(lightOn ? "ON" : "OFF", name + "/lig");
        return updated;
    }

    public void handle(String room, String payload) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }
}
