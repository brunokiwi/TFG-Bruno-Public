package com.casa.iot.backend.mqtt;
import org.springframework.stereotype.Component;

import com.casa.iot.backend.service.LightService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.SoundService;

@Component
public class MqttEventHandler {
    private final LightService lightService;
    private final MovementService movementService;
    private final SoundService soundService;

    public MqttEventHandler(LightService lightService, MovementService movementService, SoundService soundService) {
        this.lightService = lightService;
        this.movementService = movementService;
        this.soundService = soundService;
    }

    public void handleMessage(String topic, String payload) {
        String[] parts = topic.split("/");
        if (parts.length < 2) return;
        String room = parts[0];
        String subtopic = parts[1];
        
        switch (subtopic) { // TODO: implementar y enum?
            case "lig":
                lightService.handle(room, payload);
                break;
            case "mov":
                movementService.handle(room, payload);
                break;
            case "sou":
                soundService.handle(room, payload);
                break;
        }
    }
}

