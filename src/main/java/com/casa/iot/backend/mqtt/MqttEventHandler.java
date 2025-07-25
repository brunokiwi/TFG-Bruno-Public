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
        if (parts.length < 3) {
            if (parts.length >= 2) {
                String room = parts[0];
                movementService.handle(room, payload);
            }
            return;
        }
        
        String room = parts[0];
        String subsystem = parts[1]; // lig, mov, sou, rfid
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
            case "rfid":
                if ("event".equals(messageType)) {
                    rfidService.handle(room, payload);
                }
                break;
            default:
                System.out.println("Subsistema no reconocido: " + subsystem);
        }
    }
}

