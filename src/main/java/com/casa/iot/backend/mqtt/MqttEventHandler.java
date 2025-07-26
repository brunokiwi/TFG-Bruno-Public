package com.casa.iot.backend.mqtt;
import org.springframework.stereotype.Component;

import com.casa.iot.backend.service.LightService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.RFIDService;
import com.casa.iot.backend.service.SoundService;

@Component
public class MqttEventHandler {
    private final LightService lightService;
    private final MovementService movementService;
    private final SoundService soundService;
    private final RFIDService rfidService;

    public MqttEventHandler(LightService lightService, MovementService movementService, 
                           SoundService soundService, RFIDService rfidService) {
        this.lightService = lightService;
        this.movementService = movementService;
        this.soundService = soundService;
        this.rfidService = rfidService;
    }

    public void handleMessage(String topic, String payload) {
        System.out.println("MQTT recibido - Topic: " + topic + ", Payload: " + payload);
        
        String[] parts = topic.split("/");
        String room = parts[0];
        if (room.equals("rfid")){
            handleRFID(parts, payload);
            return;
        }

        if (parts.length < 3) {
            if (parts.length >= 2) 
                movementService.handle(room, payload);
            return;
        }
        
        String subsystem = parts[1]; // lig, mov, sou
        String messageType = parts[2]; // event, confirmation, command
        
        // IGNORAR comandos que nosotros mismos enviamos
        if ("command".equals(messageType)) {
            System.out.println("Ignorando comando saliente: " + topic);
            return;
        }
        
        switch (subsystem) {
            case "lig":
                if ("confirmation".equals(messageType)) {
                    lightService.handleConfirmation(room, payload);
                } else if ("event".equals(messageType)) {
                    lightService.handle(room, payload);
                }
                break;
            case "mov":
                if ("confirmation".equals(messageType)) {
                    movementService.handleConfirmation(room, payload);
                } else if ("event".equals(messageType)) {
                    movementService.handle(room, payload);
                }
                break;
            case "sou":
                break;
            default:
                System.out.println("Subsistema no reconocido: " + subsystem);
        }
    }
    
    private void handleRFID(String[] parts, String payload) {
        String subsystem = parts[1];
        switch (subsystem) {
            case "event":
                rfidService.handle(payload);
                break;
            case "register":
                System.out.println("Iniciando registro RFID para usuario: " + payload);
                rfidService.handleRegister(payload);
                break;
            case "command":
                System.out.println("Ignorando comando saliente de rfid");
                break;
        }
    }
}

