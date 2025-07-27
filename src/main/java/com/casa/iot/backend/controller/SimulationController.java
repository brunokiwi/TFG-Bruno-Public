package com.casa.iot.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casa.iot.backend.mqtt.MqttEventHandler;

@RestController
@RequestMapping("/simulation")
public class SimulationController {
    
    private final MqttEventHandler mqttEventHandler;

    public SimulationController(MqttEventHandler mqttEventHandler) {
        this.mqttEventHandler = mqttEventHandler;
    }

    // ========== SIMULACIONES DE EVENTOS (lo que ya tienes) ==========
    
    @PostMapping("/movement-detected/{roomName}")
    public Map<String, String> simulateMovement(@PathVariable String roomName) {
        String payload = String.format(
            "{\"event\":\"MOVEMENT_DETECTED\",\"timestamp\":\"%s\"}", 
            java.time.LocalDateTime.now()
        );
        
        String topic = roomName + "/mov/event";
        mqttEventHandler.handleMessage(topic, payload);
        
        return Map.of(
            "message", "Movimiento simulado",
            "room", roomName,
            "payload", payload
        );
    }

    
    @PostMapping("/device-response/alarm/{roomName}")
    public Map<String, String> simulateAlarmResponse(
            @PathVariable String roomName,
            @RequestParam String state,
            @RequestParam(defaultValue = "SUCCESS") String status) {
        
        String payload = String.format(
            "{\"status\":\"%s\",\"state\":\"%s\"}", 
            status, state
        );
        
        String topic = roomName + "/mov/confirmation";
        mqttEventHandler.handleMessage(topic, payload);
        
        return Map.of(
            "message", "Respuesta de sensor simulada",
            "room", roomName,
            "state", state,
            "status", status,
            "payload", payload
        );
    }

    @PostMapping("/device-response/light/{roomName}")
    public Map<String, String> simulateLightResponse(
            @PathVariable String roomName,
            @RequestParam String state,
            @RequestParam(defaultValue = "SUCCESS") String status) {
        
        String payload = String.format(
            "{\"status\":\"%s\",\"state\":\"%s\"}", 
            status, state
        );
        
        String topic = roomName + "/lig/confirmation";
        mqttEventHandler.handleMessage(topic, payload);
        
        return Map.of(
            "message", "Respuesta de luz simulada",
            "room", roomName,
            "state", state,
            "status", status,
            "payload", payload
        );
    }

    // ========== SIMULACIONES DE EVENTOS MANUALES DEL DISPOSITIVO ==========
    
    @PostMapping("/device-event/alarm-changed/{roomName}")
    public Map<String, String> simulateManualAlarmChange(
            @PathVariable String roomName,
            @RequestParam String state) {
        
        String payload = String.format(
            "{\"event\":\"ALARM_CHANGED\",\"state\":\"%s\",\"source\":\"MANUAL\",\"timestamp\":\"%s\"}", 
            state, java.time.LocalDateTime.now()
        );
        
        String topic = roomName + "/mov/event";
        mqttEventHandler.handleMessage(topic, payload);
        
        return Map.of(
            "message", "Cambio manual de alarma simulado",
            "room", roomName,
            "state", state,
            "payload", payload
        );
    }

    @PostMapping("/device-event/light-changed/{roomName}")
    public Map<String, String> simulateManualLightChange(
            @PathVariable String roomName,
            @RequestParam String state) {
        
        String payload = String.format(
            "{\"event\":\"LIGHT_CHANGED\",\"state\":\"%s\",\"source\":\"MANUAL\",\"timestamp\":\"%s\"}", 
            state, java.time.LocalDateTime.now()
        );
        
        String topic = roomName + "/lig/event";
        mqttEventHandler.handleMessage(topic, payload);
        
        return Map.of(
            "message", "Cambio manual de luz simulado",
            "room", roomName,
            "state", state,
            "payload", payload
        );
    }

    // ========== SIMULAR ERROR DEL DISPOSITIVO ==========
    
    @PostMapping("/device-error/{roomName}")
    public Map<String, String> simulateDeviceError(
            @PathVariable String roomName,
            @RequestParam String deviceType, // "alarm" o "light"
            @RequestParam String error) {
        
        String payload = String.format(
            "{\"status\":\"ERROR\",\"error\":\"%s\"}", 
            error
        );
        
        String subsystem = "alarm".equals(deviceType) ? "mov" : "lig";
        String topic = roomName + "/" + subsystem + "/confirmation";
        mqttEventHandler.handleMessage(topic, payload);
        
        return Map.of(
            "message", "Error de dispositivo simulado",
            "room", roomName,
            "deviceType", deviceType,
            "error", error,
            "payload", payload
        );
    }
}