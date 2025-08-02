package com.casa.iot.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.casa.iot.backend.mqtt.MqttEventHandler;
import com.casa.iot.backend.service.LightService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.RFIDService;
import com.casa.iot.backend.service.SoundService;

class MqttEventHandlerTest {

    private LightService lightService;
    private MovementService movementService;
    private SoundService soundService;
    private RFIDService rfidService;
    private MqttEventHandler handler;

    @BeforeEach
    void setup() {
        lightService = mock(LightService.class);
        movementService = mock(MovementService.class);
        soundService = mock(SoundService.class);
        rfidService = mock(RFIDService.class);
        handler = new MqttEventHandler(lightService, movementService, soundService, rfidService);
    }

    @Test
    void testHandleMessage_LightConfirmation() {
        handler.handleMessage("kitchen/lig/confirmation", "{\"status\":\"SUCCESS\"}");
        verify(lightService).handleConfirmation(eq("kitchen"), anyString());
    }

    @Test
    void testHandleMessage_MovementEvent() {
        handler.handleMessage("salon/mov/event", "{\"event\":\"MOVEMENT_DETECTED\"}");
        verify(movementService).handle(eq("salon"), anyString());
    }

    @Test
    void testHandleMessage_MovementConfirmation() {
        handler.handleMessage("salon/mov/confirmation", "{\"status\":\"SUCCESS\"}");
        verify(movementService).handleConfirmation(eq("salon"), anyString());
    }

    @Test
    void testHandleMessage_RFIDEvent() {
        handler.handleMessage("rfid/event", "{\"event\":\"RFID_DETECTED\"}");
        verify(rfidService).handle(anyString());
    }

    @Test
    void testHandleMessage_RFIDRegister() {
        handler.handleMessage("rfid/register", "{\"username\":\"test\"}");
        verify(rfidService).handleRegister(anyString());
    }

    @Test
    void testHandleMessage_RFIDCommand() {
        handler.handleMessage("rfid/command", "{\"cmd\":\"test\"}");
        verifyNoInteractions(lightService, movementService, soundService);
    }

    @Test
    void testHandleMessage_UnknownSubsystem() {
        handler.handleMessage("kitchen/unknown/event", "{}");
    }

    @Test
    void testHandleMessage_ShortTopic_Movement() {
        handler.handleMessage("salon/mov", "{\"event\":\"MOVEMENT_DETECTED\"}");
        verify(movementService).handle(eq("salon"), anyString());
    }

    @Test
    void testHandleMessage_CommandIgnored() {
        handler.handleMessage("kitchen/lig/command", "{\"cmd\":\"ON\"}");
        verifyNoInteractions(lightService, movementService, soundService, rfidService);
    }
}