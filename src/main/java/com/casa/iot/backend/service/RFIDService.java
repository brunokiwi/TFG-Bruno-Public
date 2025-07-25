package com.casa.iot.backend.service;

import org.springframework.stereotype.Service;

import com.casa.iot.backend.repository.RoomRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class RFIDService {
    
    private final MovementService movementService;
    private final RoomRepository roomRepository;
    private final EventLogService eventLogService;
    private final VacationModeService vacationModeService;

    public RFIDService(MovementService movementService, RoomRepository roomRepository, EventLogService eventLogService, VacationModeService vacationModeService) {
        this.movementService = movementService;
        this.roomRepository = roomRepository;
        this.eventLogService = eventLogService;
        this.vacationModeService = vacationModeService;
    }

    public void handle(String room, String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String event = json.get("event").getAsString();
            
            if ("RFID_DETECTED".equals(event)) {
                String cardId = json.get("cardId").getAsString();
                System.out.println("Tarjeta RFID detectada: " + cardId + " en " + room);
                disableAllMovementSensors(cardId);
                if (vacationModeService.isVacationModeActive()) {
                    vacationModeService.deactivateVacationMode();
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando evento RFID: " + e.getMessage());
        }
    }

    private void disableAllMovementSensors(String cardId) {
        roomRepository.findAll().forEach(roomEntity -> {
            movementService.sendAlarmCommand(roomEntity.getName(), false);
        });
        
        // logging
        String details = String.format("{\"cardId\":\"%s\",\"action\":\"DISABLE_ALL_SENSORS\",\"timestamp\":\"%s\"}", 
                                     cardId, java.time.LocalDateTime.now());
        eventLogService.logSystemAction("RFID_ALL_SENSORS_OFF", null, details, "RFID");
        
        System.out.println("Todos los sensores de movimiento desactivados por RFID: " + cardId);
    }
}