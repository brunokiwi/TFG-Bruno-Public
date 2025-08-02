package com.casa.iot.backend;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.mqtt.MqttGateway;
import com.casa.iot.backend.repository.RoomRepository;
import com.casa.iot.backend.service.RoomService;

class RoomServiceTest {

    private RoomRepository repo;
    private MqttGateway gateway;
    private RoomService svc;

    @BeforeEach
    void setUp() {
        repo = mock(RoomRepository.class);
        gateway = mock(MqttGateway.class);
        svc = new RoomService(repo, gateway);
    }

    @Test
    void createRoomWhenNotExists() {
        when(repo.findById("test")).thenReturn(Optional.empty());
        when(repo.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        Room r = svc.createRoom("test");
        assertEquals("test", r.getName());
        verify(repo).save(r);
    }

    @Test
    void createRoomWhenAlreadyExistsReturnsExisting() {
        Room existing = new Room("test");
        when(repo.findById("test")).thenReturn(Optional.of(existing));
        Room r = svc.createRoom("test");
        assertEquals(existing, r);
        verify(repo, never()).save(any());
    }

    @Test
    void getRoomByNameWhenExists() {
        Room existing = new Room("salon");
        when(repo.findById("salon")).thenReturn(Optional.of(existing));

        assertEquals(existing, svc.getRoomByName("salon"));
    }

    @Test
    void getAllRoomsReturnsList() {
        Room r1 = new Room("a");
        Room r2 = new Room("b");
        when(repo.findAll()).thenReturn(List.of(r1, r2));
        List<Room> rooms = svc.getAllRooms();
        assertEquals(2, rooms.size());
    }

}