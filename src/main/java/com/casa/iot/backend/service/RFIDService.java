package com.casa.iot.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;
import com.casa.iot.backend.repository.UserRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class RFIDService {
    private final MovementService movementService;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final EventLogService eventLogService;
    private final VacationModeService vacationModeService;
    private final MqttGateway mqttGateway;
    private String pendingRegistrationUser = null;

    @Autowired
    private NotificationService notificationService;

    public RFIDService(MovementService movementService, RoomRepository roomRepository, EventLogService eventLogService, VacationModeService vacationModeService, UserRepository userRepository, MqttGateway mqttGateway) {
        this.movementService = movementService;
        this.roomRepository = roomRepository;
        this.eventLogService = eventLogService;
        this.vacationModeService = vacationModeService;
        this.userRepository = userRepository;
        this.mqttGateway = mqttGateway;
    }

    public void handle(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String event = json.get("event").getAsString();

            if ("RFID_DETECTED".equals(event)) {
                String cardId = json.get("cardId").getAsString();
                // Verificar si la tarjeta está registrada para algún usuario
                var userOpt = userRepository.findByRfidUid(cardId);
                if (userOpt.isPresent()) {
                    System.out.println("Tarjeta RFID detectada: " + cardId + " (usuario: " + userOpt.get().getUsername() + ")");
                    disableAllMovementSensors(cardId);
                    if (vacationModeService.isVacationModeActive()) {
                        vacationModeService.deactivateVacationMode();
                    }
                } else {
                    System.out.println("Tarjeta RFID detectada: " + cardId + " (NO registrada para ningún usuario)");
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando evento RFID: " + e.getMessage());
        }
    }

    public void startRfidRegistration(String username) {
        pendingRegistrationUser = username;
        mqttGateway.sendToMqtt("", "rfid/command/START");
        System.out.println("Comando rfid/command/START enviado para iniciar registro RFID para usuario: " + username);
    }

    public void cancelRfidRegistration(String username) {
        pendingRegistrationUser = null;
        mqttGateway.sendToMqtt("", "rfid/command/CANCEL");
    }

    public void handleRegister(String payload) {
        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            String event = json.get("event").getAsString();

            if ("RFID_REGISTER".equals(event)) {
                String cardId = json.get("cardId").getAsString();
                System.out.println("Tarjeta RFID detectada para registro: " + cardId + ", usuario pendiente: " + pendingRegistrationUser);
                if (pendingRegistrationUser != null) {
                    var userOpt = userRepository.findByUsername(pendingRegistrationUser);
                    if (userOpt.isPresent()) {
                        var user = userOpt.get();
                        user.setRfidUid(cardId);
                        userRepository.save(user);
                        System.out.println("UID " + cardId + " guardado para usuario " + pendingRegistrationUser);
                    } else {
                        System.err.println("Usuario para registro RFID no encontrado: " + pendingRegistrationUser);
                    }
                    pendingRegistrationUser = null;
                } else {
                    System.err.println("No hay usuario pendiente de registro RFID.");
                }
            } else if ("RFID_REGISTER_CANCEL".equals(event)) {
                System.out.println("Registro RFID cancelado por el dispositivo IoT.");
                pendingRegistrationUser = null;
             
            }
        } catch (Exception e) {
            System.err.println("Error procesando registro RFID: " + e.getMessage());
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