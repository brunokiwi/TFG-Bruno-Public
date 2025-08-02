package com.casa.iot.backend;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;
import com.casa.iot.backend.service.EventLogService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.NotificationService;

class MovementServiceTest {

    private RoomRepository roomRepo;
    private MqttGateway mqttGateway;
    private NotificationService notificationService;
    private EventLogService eventLogService;
    private MovementService svc;

    @BeforeEach
    void setUp() {
        roomRepo = mock(RoomRepository.class);
        mqttGateway = mock(MqttGateway.class);
        notificationService = mock(NotificationService.class);
        eventLogService = mock(EventLogService.class);
        svc = new MovementService(roomRepo, mqttGateway, notificationService, eventLogService);
    }

    @Test
    void sendAlarmCommandSendsMqtt() {
        svc.sendAlarmCommand("kitchen", true);
        verify(mqttGateway).sendToMqtt(anyString(), eq("kitchen/mov/command"));
    }

    @Test
    void handleConfirmationSuccessUpdatesRoom() {
        Room room = new Room("kitchen");
        when(roomRepo.findById("kitchen")).thenReturn(Optional.of(room));
        svc.handleConfirmation("kitchen", "{\"status\":\"SUCCESS\",\"state\":\"ON\"}");
        verify(roomRepo).save(room);
    }

    @Test
    void handleConfirmationErrorLogsError() {
        Room room = new Room("kitchen");
        when(roomRepo.findById("kitchen")).thenReturn(Optional.of(room));
        assertDoesNotThrow(() -> 
            svc.handleConfirmation("kitchen", "{\"status\":\"ERROR\",\"state\":\"OFF\",\"error\":\"fail\"}")
        );
        verify(roomRepo, org.mockito.Mockito.never()).save(room);
    }

    @Test
    void handleConfirmationMalformedJsonDoesNotThrow() {
        svc.handleConfirmation("kitchen", "not a json");
        // test pasa sin o hay excepcion
    }

    @Test
    void handleMovementDetectedWithActiveSensorSendsNotification() {
        Room room = new Room("kitchen");
        room.setDetectOn(true);
        when(roomRepo.findById("kitchen")).thenReturn(Optional.of(room));
        String payload = "{\"event\":\"MOVEMENT_DETECTED\"}";
        svc.handle("kitchen", payload);
        verify(notificationService).sendMovementAlert("kitchen");
    }

    @Test
    void handleMovementDetectedWithInactiveSensorDoesNotSendNotification() {
        Room room = new Room("kitchen");
        room.setDetectOn(false);
        when(roomRepo.findById("kitchen")).thenReturn(Optional.of(room));
        String payload = "{\"event\":\"MOVEMENT_DETECTED\"}";
        svc.handle("kitchen", payload);
        // No se debe llamar a sendMovementAlert
        verify(notificationService, org.mockito.Mockito.never()).sendMovementAlert("kitchen");
    }

    @Test
    void handleAlarmChangedUpdatesRoom() {
        Room room = new Room("kitchen");
        when(roomRepo.findById("kitchen")).thenReturn(Optional.of(room));
        String payload = "{\"event\":\"ALARM_CHANGED\",\"state\":\"ON\"}";
        svc.handle("kitchen", payload);
        verify(roomRepo).save(room);
    }

    @Test
    void handleAlarmChangedCreatesRoomIfNotExists() {
        when(roomRepo.findById("kitchen")).thenReturn(Optional.empty());
        String payload = "{\"event\":\"ALARM_CHANGED\",\"state\":\"OFF\"}";
        svc.handle("kitchen", payload);
        verify(roomRepo).save(org.mockito.ArgumentMatchers.any(Room.class));
    }

    @Test
    void handleMalformedJsonDoesNotThrow() {
        svc.handle("kitchen", "not a json");
        // No excepción lanzada, test pasa si no hay crash
    }
}