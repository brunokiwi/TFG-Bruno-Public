package com.casa.iot.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;
import com.casa.iot.backend.service.LightService;

class LightServiceTest {

    private RoomRepository repo;
    private LightService svc;
    private MqttGateway mqttGateway;

    @BeforeEach
    void setUp() {
        repo = mock(RoomRepository.class);
        mqttGateway = mock(MqttGateway.class);
        svc = new LightService(repo, mqttGateway);
    }

    @Test
    void handleConfirmationSuccess() {
        Room room = new Room("kitchen");
        when(repo.findById("kitchen")).thenReturn(java.util.Optional.of(room));
        svc.handleConfirmation("kitchen", "{\"status\":\"SUCCESS\",\"state\":\"ON\"}");
        verify(repo).save(room);
        assertTrue(room.isLightOn());
    }

    @Test
    void handleConfirmationError() {
        svc.handleConfirmation("kitchen", "{\"status\":\"ERROR\",\"error\":\"fail\"}");
        // No save should be called
        verify(repo, never()).save(any());
    }

    @Test
    void sendLightCommandSendsMqtt() {
        svc.sendLightCommand("kitchen", true);
        verify(mqttGateway).sendToMqtt(anyString(), eq("kitchen/lig/command"));
    }
}