package com.casa.iot.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.model.RoomSchedule;
import com.casa.iot.backend.service.RoomScheduleService;
import com.casa.iot.backend.service.RoomService;

@RestController
@RequestMapping("/rooms/{roomName}/schedules")
public class RoomScheduleController {
    private final RoomScheduleService scheduleService;
    private final RoomService roomService;

    public RoomScheduleController(RoomScheduleService scheduleService, RoomService roomService) {
        this.scheduleService = scheduleService;
        this.roomService = roomService;
    }

    @PostMapping
    public RoomSchedule createSchedule(
            @PathVariable String roomName,
            @RequestParam String type, // "light" o "alarm"
            @RequestParam boolean state,
            @RequestParam(required = false) String name,      // Nombre opcional del schedule
            @RequestParam(required = false) String time,      // formato "HH:mm", para puntual
            @RequestParam(required = false) String startTime, // formato "HH:mm", para intervalo
            @RequestParam(required = false) String endTime    // formato "HH:mm", para intervalo
    ) {
        Room room = roomService.getRoomByName(roomName);
        if (room == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");

        RoomSchedule schedule;
        if (time != null) {
            // hora exacta + accion
            schedule = new RoomSchedule(room, type, state, java.time.LocalTime.parse(time));
        } else if (startTime != null && endTime != null) {
            // accion en intervalo
            schedule = new RoomSchedule(room, type, state, null);
            schedule.setStartTime(java.time.LocalTime.parse(startTime));
            schedule.setEndTime(java.time.LocalTime.parse(endTime));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes especificar 'time' o 'startTime' y 'endTime'");
        }
        
        // Establecer el nombre si se proporciona
        if (name != null && !name.trim().isEmpty()) {
            schedule.setName(name.trim());
        }
        
        return scheduleService.createSchedule(schedule);
    }

    // Ejemplo de peticiones:
    // POST http://localhost:8080/rooms/salon/schedules?type=light&state=true&time=20:00
    // POST http://localhost:8080/rooms/salon/schedules?type=alarm&state=true&startTime=22:00&endTime=06:00

    @GetMapping
    public List<RoomSchedule> getSchedules(@PathVariable String roomName) {
        return scheduleService.getSchedulesForRoom(roomName);
    }

    @DeleteMapping("/{id}")
    public void deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
    }
}