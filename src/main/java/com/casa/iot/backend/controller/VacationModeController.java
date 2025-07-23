package com.casa.iot.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casa.iot.backend.service.EventLogService;
import com.casa.iot.backend.service.VacationModeService;

@RestController
@RequestMapping("/vacation-mode")
public class VacationModeController {
    
    private final VacationModeService vacationModeService;
    private final EventLogService eventLogService;
    
    public VacationModeController(VacationModeService vacationModeService, EventLogService eventLogService) {
        this.vacationModeService = vacationModeService;
        this.eventLogService = eventLogService;
    }
    
    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activateVacationMode(
            @RequestParam(required = false, defaultValue = "unknown") String userId) {
        
        vacationModeService.activateVacationMode();
        
        String details = String.format("{\"method\":\"API\",\"timestamp\":\"%s\",\"sensors\":\"ALL_ACTIVATED\"}", 
                                     java.time.LocalDateTime.now());
        eventLogService.logVacationModeActivated(userId, details);
        
        return ResponseEntity.ok(Map.of(
            "message", "Modo vacaciones activado",
            "active", true
        ));
    }
    
    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateVacationMode(
            @RequestParam(required = false, defaultValue = "unknown") String userId) {
        
        vacationModeService.deactivateVacationMode();
        
        String details = String.format("{\"method\":\"API\",\"timestamp\":\"%s\"}", 
                                     java.time.LocalDateTime.now());
        eventLogService.logVacationModeDeactivated(userId, details);
        
        return ResponseEntity.ok(Map.of(
            "message", "Modo vacaciones desactivado",
            "active", false
        ));
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getVacationModeStatus() {
        boolean isActive = vacationModeService.isVacationModeActive();
        return ResponseEntity.ok(Map.of("active", isActive));
    }
}
