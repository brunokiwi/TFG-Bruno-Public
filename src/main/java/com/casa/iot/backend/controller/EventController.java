package com.casa.iot.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casa.iot.backend.model.Event;
import com.casa.iot.backend.service.EventLogService;

@RestController
@RequestMapping("/events")
public class EventController {
    
    private final EventLogService eventLogService;
    
    public EventController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }
    
    @GetMapping("/recent")
    public List<Event> getRecentEvents(@RequestParam(defaultValue = "24") int hours) {
        return eventLogService.getRecentEvents(hours);
    }
    
    @GetMapping
    public Page<Event> getEvents(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        LocalDateTime startDate = start != null ? LocalDateTime.parse(start) : LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = end != null ? LocalDateTime.parse(end) : LocalDateTime.now();
        
        return eventLogService.getEventsByDateRange(startDate, endDate, page, size);
    }
    
    @GetMapping("/room/{roomName}")
    public Page<Event> getRoomEvents(
            @PathVariable String roomName,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        LocalDateTime startDate = start != null ? LocalDateTime.parse(start) : LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = end != null ? LocalDateTime.parse(end) : LocalDateTime.now();
        
        return eventLogService.getRoomEvents(roomName, startDate, endDate, page, size);
    }
    
    @GetMapping("/user/{userId}")
    public Page<Event> getUserEvents(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return eventLogService.getUserEvents(userId, page, size);
    }
    
    @GetMapping("/failed-logins/{username}")
    public List<Event> getFailedLogins(
            @PathVariable String username,
            @RequestParam(defaultValue = "24") int hours
    ) {
        return eventLogService.getFailedLoginAttempts(username, hours);
    }
    
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> manualCleanup(
            @RequestParam(defaultValue = "30") int olderThanDays
    ) {
        // endpoint de limpieza manual TODO solo admins
        
        return ResponseEntity.ok(Map.of(
            "message", "La limpieza automática se ejecuta diariamente a las 2:00 AM",
            "current_retention_days", 30,
            "next_cleanup", "Tonight at 2:00 AM"
        ));
    }
}