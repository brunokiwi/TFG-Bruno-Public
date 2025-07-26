package com.casa.iot.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final MqttGateway mqttGateway; // to send messages to iOT

    public RoomService(RoomRepository repo, MqttGateway gateway) {
        this.roomRepository = repo;
        this.mqttGateway = gateway;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Room getRoomByName(String name) {
        return roomRepository.findById(name).orElse(null);
    }

    public Room createRoom(String name) {
        return roomRepository.findById(name).orElseGet(() -> roomRepository.save(new Room(name)));
    }

    public void removeRoom(String roomName) {
        Room room = roomRepository.findById(roomName).orElse(null);
        if (room != null) {
            roomRepository.delete(room);
            mqttGateway.sendToMqtt(roomName + "/remove");
        }
    }
}
