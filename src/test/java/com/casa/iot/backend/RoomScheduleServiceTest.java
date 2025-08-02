package com.casa.iot.backend;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.model.RoomSchedule;
import com.casa.iot.backend.repository.RoomScheduleRepository;
import com.casa.iot.backend.service.RoomScheduleService;

class RoomScheduleServiceTest {

    private RoomScheduleRepository scheduleRepository;
    private RoomScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(RoomScheduleRepository.class);
        scheduleService = new RoomScheduleService(scheduleRepository);
    }

    @Test
    void testCreateSchedule() {
        Room room = new Room("salon");
        RoomSchedule schedule = new RoomSchedule(room, "light", true, LocalTime.of(20, 0));
        when(scheduleRepository.save(schedule)).thenReturn(schedule);

        RoomSchedule result = scheduleService.createSchedule(schedule);

        assertEquals(schedule, result);
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void testGetSchedulesForRoom() {
        Room room = new Room("salon");
        RoomSchedule schedule = new RoomSchedule(room, "light", true, LocalTime.of(20, 0));
        when(scheduleRepository.findByRoom_Name("salon")).thenReturn(Collections.singletonList(schedule));

        List<RoomSchedule> result = scheduleService.getSchedulesForRoom("salon");

        assertEquals(1, result.size());
        assertEquals("salon", result.get(0).getRoom().getName());
    }

    @Test
    void testDeleteSchedule() {
        scheduleService.deleteSchedule(1L);
        verify(scheduleRepository).deleteById(1L);
    }

    @Test
    void testGetSchedulesForTime() {
        Room room = new Room("salon");
        RoomSchedule schedule = new RoomSchedule(room, "light", true, LocalTime.of(20, 0));
        when(scheduleRepository.findByScheduleTypeAndTimeHourAndTimeMinute("puntual", 20, 0)).thenReturn(Collections.singletonList(schedule));

        List<RoomSchedule> result = scheduleService.getPunctualSchedulesForTime(LocalTime.of(20, 0));

        assertEquals(1, result.size());
        assertEquals(LocalTime.of(20, 0), result.get(0).getTime());
    }

    @Test
    void testGetAllSchedulesReturnsList() {
        Room room = new Room("salon");
        RoomSchedule schedule = new RoomSchedule(room, "alarm", true, LocalTime.of(8, 0));
        when(scheduleRepository.findAll()).thenReturn(List.of(schedule));

        List<RoomSchedule> result = scheduleService.getAllSchedules();

        assertEquals(1, result.size());
        assertEquals("alarm", result.get(0).getType());
    }

    @Test
    void testGetAllSchedulesReturnsEmpty() {
        when(scheduleRepository.findAll()).thenReturn(Collections.emptyList());
        List<RoomSchedule> result = scheduleService.getAllSchedules();
        assertTrue(result.isEmpty());
    }
}
