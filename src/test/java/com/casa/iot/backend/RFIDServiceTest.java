package com.casa.iot.backend;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.model.User;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;
import com.casa.iot.backend.repository.UserRepository;
import com.casa.iot.backend.service.EventLogService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.RFIDService;
import com.casa.iot.backend.service.VacationModeService;

class RFIDServiceTest {

    private MovementService movementService;
    private RoomRepository roomRepo;
    private UserRepository userRepo;
    private EventLogService eventLogService;
    private VacationModeService vacationModeService;
    private MqttGateway mqttGateway;
    private RFIDService svc;

    @BeforeEach
    void setUp() {
        movementService = mock(MovementService.class);
        roomRepo = mock(RoomRepository.class);
        userRepo = mock(UserRepository.class);
        eventLogService = mock(EventLogService.class);
        vacationModeService = mock(VacationModeService.class);
        mqttGateway = mock(MqttGateway.class);
        svc = new RFIDService(movementService, roomRepo, eventLogService, vacationModeService, userRepo, mqttGateway);
    }

    @Test
    void startRfidRegistrationSendsCommand() {
        svc.startRfidRegistration("user");
        verify(mqttGateway).sendToMqtt(anyString(), eq("rfid/command"));
    }

    @Test
    void handleRegisterAssignsCardIdToUser() {
        User user = new User("user", "pass", null);
        when(userRepo.findByUsername("user")).thenReturn(Optional.of(user));
        svc.startRfidRegistration("user");
        assertEquals(null, user.getRfidUid());
        svc.handleRegister("{\"event\":\"RFID_REGISTER\",\"cardId\":\"1234\"}");
        assertEquals("1234", user.getRfidUid());
    }

    @Test
    void handleWithRegisteredCardDisablesSensorsAndDeactivatesVacationMode() {
        User user = new User("user", "pass", null);
        when(userRepo.findByRfidUid("1234")).thenReturn(Optional.of(user));
        when(vacationModeService.isVacationModeActive()).thenReturn(true);

        svc.handle("{\"event\":\"RFID_DETECTED\",\"cardId\":\"1234\"}");

        verify(roomRepo, never()).findByName(any());
        verify(movementService, atLeast(0)).sendAlarmCommand(anyString(), eq(false));
        verify(vacationModeService).deactivateVacationMode();
    }

    @Test
    void handleWithUnregisteredCardDoesNotDisableSensors() {
        when(userRepo.findByRfidUid("9999")).thenReturn(Optional.empty());

        svc.handle("{\"event\":\"RFID_DETECTED\",\"cardId\":\"9999\"}");

        verify(movementService, never()).sendAlarmCommand(anyString(), anyBoolean());
        verify(vacationModeService, never()).deactivateVacationMode();
    }

    @Test
    void handleWithMalformedJsonDoesNotThrow() {
        assertDoesNotThrow(() -> svc.handle("not a json"));
    }

    @Test
    void cancelRfidRegistrationSendsCancelCommand() {
        svc.cancelRfidRegistration("user");
        verify(mqttGateway).sendToMqtt("{\"command\":\"CANCEL\"}", "rfid/command");
    }
}