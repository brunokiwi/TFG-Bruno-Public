package com.casa.iot.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.service.LightService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.RoomService;
import com.casa.iot.backend.service.SoundService;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final LightService lightService;
    private final MovementService movementService;
    private final SoundService soundService;
    private final RoomService roomService;

    public RoomController(RoomService roomService, LightService lightService, MovementService movementService, SoundService soundService) {
        this.roomService = roomService;
        this.lightService = lightService;
        this.movementService = movementService;
        this.soundService = soundService;
    }

    @GetMapping
    public List<Room> getRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/{roomName}")
    public Room getRoom(@PathVariable String roomName) {
        return roomService.getRoomByName(roomName);
    }

    @PostMapping("/{roomName}")
    public Room createRoom(@PathVariable String roomName) {
        // si room no existe, crearla con estado por defecto
        return roomService.createRoom(roomName);
    }

    @PostMapping("/{roomName}/light")
    public Room updateLight(
            @PathVariable String roomName,
            @RequestParam boolean state
    ) {
        // si room no eixte, crearla con ese estado, sino cambiar el estado TODO se deberia crear?
        return lightService.updateLight(roomName, state);
    }

    @PostMapping("/{roomName}/alarm")
    public Room updateAlarm(
            @PathVariable String roomName,
            @RequestParam boolean state
    ) {
        // si room no eixte, crearla con ese estado, sino cambiar el estado TODO se deberia crear?
        return movementService.updateAlarm(roomName, state);
    }

    @PostMapping("/{roomName}/remove")
    public void removeRoom(@PathVariable String roomName) {
        roomService.removeRoom(roomName);
    }
}
