package com.casa.iot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casa.iot.backend.model.Room;

public interface RoomRepository extends JpaRepository<Room, String> {
    Room findByName(String name); // útil para buscar por nombre
}
