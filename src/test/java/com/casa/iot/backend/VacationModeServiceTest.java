package com.casa.iot.backend;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.repository.RoomRepository;
import com.casa.iot.backend.service.EventLogService;
import com.casa.iot.backend.service.LightService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.VacationModeService;

class VacationModeServiceTest {

    private RoomRepository roomRepo;
    private MovementService movementService;
    private LightService lightService;
    private EventLogService eventService;
    private VacationModeService svc;

    @BeforeEach
    void setUp() {
        roomRepo = mock(RoomRepository.class);
        movementService = mock(MovementService.class);
        lightService = mock(LightService.class);
        eventService = mock(EventLogService.class);
        svc = new VacationModeService(roomRepo, movementService, lightService, eventService);
    }

    @Test
    void activateSetsFlagAndSendsCommands() {
        List<Room> rooms = List.of(new Room("A"), new Room("B"));
        when(roomRepo.findAll()).thenReturn(rooms);

        svc.activateVacationMode();

        assertTrue(svc.isVacationModeActive());
        verify(movementService).sendAlarmCommand("A", true);
        verify(movementService).sendAlarmCommand("B", true);
    }

    @Test
    void deactivateClearsFlag() {
        svc.activateVacationMode();
        assertTrue(svc.isVacationModeActive());

        svc.deactivateVacationMode();
        assertFalse(svc.isVacationModeActive());
    }
}