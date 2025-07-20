package com.casa.iot.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.casa.iot.backend.service.VacationModeService;

@RestController
@RequestMapping("/vacation-mode")
public class VacationModeController {
    
    private final VacationModeService vacationModeService;
    
    public VacationModeController(VacationModeService vacationModeService) {
        this.vacationModeService = vacationModeService;
    }
    
    @PostMapping("/activate")
    public ResponseEntity<Map<String, Object>> activateVacationMode() {
        vacationModeService.activateVacationMode();
        return ResponseEntity.ok(Map.of(
            "message", "Modo vacaciones activado",
            "active", true
        ));
    }
    
    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateVacationMode() {
        vacationModeService.deactivateVacationMode();
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
