package com.casa.iot.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.service.AuthService;
import com.casa.iot.backend.service.LightService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.RoomService;

@RestController
@RequestMapping("/rooms")
public class RoomController {
    
    private final RoomService roomService;
    private final LightService lightService;
    private final MovementService movementService;
    private final AuthService authService;

    public RoomController(RoomService roomService, LightService lightService, MovementService movementService, AuthService authService ) {
        this.roomService = roomService;
        this.lightService = lightService;
        this.movementService = movementService;
        this.authService = authService;
    }

    @PostMapping("/{roomName}/light")
    public ResponseEntity<?> updateLight(
            @PathVariable String roomName,
            @RequestParam boolean state
    ) {
        try {
            // solo mqtt
            lightService.sendLightCommand(roomName, state);
            
            // respuesta inmediata (lazy)
            return ResponseEntity.ok(Map.of(
                "message", "Comando enviado al dispositivo",
                "roomName", roomName,
                "requestedState", state ? "ON" : "OFF",
                "status", "PENDING"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of(
                    "error", "Error al enviar comando",
                    "message", e.getMessage()
                ));
        }
    }

    @PostMapping("/{roomName}/alarm")
    public ResponseEntity<?> updateAlarm(
            @PathVariable String roomName,
            @RequestParam boolean state
    ) {
        try {
            // solo mqtt
            movementService.sendAlarmCommand(roomName, state);
            
            // respuesta inmediata (lazy)
            return ResponseEntity.ok(Map.of(
                "message", "Comando enviado al sensor",
                "roomName", roomName,
                "requestedState", state ? "ON" : "OFF",
                "status", "PENDING"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of(
                    "error", "Error al enviar comando al sensor",
                    "message", e.getMessage()
                ));
        }
    }

    @GetMapping
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/{roomName}")
    public Room getRoomByName(@PathVariable String roomName) {
        return roomService.getRoomByName(roomName);
    }

    // TODO: solo admins pueden 
    @PostMapping("/{roomName}/remove")
    public void removeRoom(@PathVariable String roomName) {
        roomService.removeRoom(roomName);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoom(
            @RequestParam String roomName,
            @RequestParam String username) {
        
        try {
            // Verificar que el usuario sea admin
            if (!authService.isAdmin(username)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Solo los administradores pueden crear habitaciones"
                ));
            }
            
            Room room = roomService.createRoom(roomName);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Habitacion creada exitosamente",
                "room", room
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al crear habitacion: " + e.getMessage()
            ));
        }
    }
}
