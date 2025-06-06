package com.casa.iot.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.repository.RoomRepository;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }

    public Room updateLuz(Long id, boolean estado) {
        Room room = roomRepository.findById(id).orElseThrow();
        room.setLuzEncendida(estado);
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
}
