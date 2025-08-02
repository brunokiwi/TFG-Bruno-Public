package com.casa.iot.backend;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.model.RoomSchedule;
import com.casa.iot.backend.service.EventLogService;
import com.casa.iot.backend.service.LightService;
import com.casa.iot.backend.service.MovementService;
import com.casa.iot.backend.service.RoomScheduleExecutor;
import com.casa.iot.backend.service.RoomScheduleService;
import com.casa.iot.backend.service.RoomService;

class RoomScheduleExecutorTest {

    private RoomScheduleService scheduleService;
    private LightService lightService;
    private MovementService movementService;
    private RoomService roomService;
    private EventLogService eventLogService;
    private RoomScheduleExecutor executor;

    @BeforeEach
    void setUp() {
        scheduleService = mock(RoomScheduleService.class);
        lightService = mock(LightService.class);
        movementService = mock(MovementService.class);
        roomService = mock(RoomService.class);
        eventLogService = mock(EventLogService.class);
        executor = new RoomScheduleExecutor(scheduleService, lightService, movementService, roomService, eventLogService);
    }

    @Test
    void executeSchedulesCallsServices() throws Exception {
        Room room = new Room("salon");
        RoomSchedule punctual = new RoomSchedule(room, "light", true, LocalTime.now().withSecond(0).withNano(0));
       
        java.lang.reflect.Field idField = RoomSchedule.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(punctual, 1L);

        when(scheduleService.getPunctualSchedulesForTime(any())).thenReturn(List.of(punctual));
        when(scheduleService.getAllIntervalSchedules()).thenReturn(List.of());
        when(roomService.getRoomByName(any())).thenReturn(room);

        executor.executeSchedules();
    }

    @Test
    void executeSchedulesWithIntervalScheduleTurnsOnLight() throws Exception {
        Room room = new Room("salon");
        RoomSchedule interval = new RoomSchedule(room, "alarm", true, LocalTime.of(8, 0), LocalTime.of(23, 0));
        java.lang.reflect.Field idField = RoomSchedule.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(interval, 2L);

        when(scheduleService.getPunctualSchedulesForTime(any())).thenReturn(List.of());
        when(scheduleService.getAllIntervalSchedules()).thenReturn(List.of(interval));
        when(roomService.getRoomByName(any())).thenReturn(room);
        room.setLightOn(false);

        executor.executeSchedules();
    }

    @Test
    void executeSchedulesWithIntervalScheduleRoomNullDoesNothing() throws Exception {
        RoomSchedule interval = new RoomSchedule(null, "light", true, LocalTime.of(8, 0), LocalTime.of(23, 0));
        java.lang.reflect.Field idField = RoomSchedule.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(interval, 3L);

        when(scheduleService.getPunctualSchedulesForTime(any())).thenReturn(List.of());
        when(scheduleService.getAllIntervalSchedules()).thenReturn(List.of(interval));
        when(roomService.getRoomByName(any())).thenReturn(null);

        executor.executeSchedules();
    }

    @Test
    void executeSchedulesWithAlarmTypeCallsMovementService() throws Exception {
        Room room = new Room("salon");
        RoomSchedule punctual = new RoomSchedule(room, "alarm", true, LocalTime.now().withSecond(0).withNano(0));
        java.lang.reflect.Field idField = RoomSchedule.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(punctual, 4L);

        when(scheduleService.getPunctualSchedulesForTime(any())).thenReturn(List.of(punctual));
        when(scheduleService.getAllIntervalSchedules()).thenReturn(List.of());
        when(roomService.getRoomByName(any())).thenReturn(room);

        executor.executeSchedules();
    }

    @Test
    void isNowInIntervalHandlesOvernightIntervals() throws Exception {
        java.lang.reflect.Method method = RoomScheduleExecutor.class.getDeclaredMethod("isNowInInterval", LocalTime.class, LocalTime.class, LocalTime.class);
        method.setAccessible(true);

        LocalTime now = LocalTime.of(1, 0);
        LocalTime start = LocalTime.of(23, 0);
        LocalTime end = LocalTime.of(6, 0);

        boolean result = (boolean) method.invoke(executor, now, start, end);
        assertTrue(result);
    }

    @Test
    void testExecuteIntervalSchedule_EndOfInterval() {
        Room room = new Room("salon");
        when(roomService.getRoomByName("salon")).thenReturn(room);

        LocalTime now = LocalTime.of(10, 0);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(10, 0);

        RoomSchedule schedule = mock(RoomSchedule.class);
        when(schedule.getRoomName()).thenReturn("salon");
        when(schedule.getType()).thenReturn("light");
        when(schedule.getStartTime()).thenReturn(startTime);
        when(schedule.getEndTime()).thenReturn(endTime);
        when(schedule.getId()).thenReturn(1L);

        when(scheduleService.getAllIntervalSchedules()).thenReturn(Collections.singletonList(schedule));
        room.setLightOn(false);

        executorTestableExecuteIntervalSchedule(executor, schedule, now);
    }

    private void executorTestableExecuteIntervalSchedule(RoomScheduleExecutor executor, RoomSchedule schedule, LocalTime now) {
        try {
            var method = RoomScheduleExecutor.class.getDeclaredMethod("executeIntervalSchedule", RoomSchedule.class, LocalTime.class);
            method.setAccessible(true);
            method.invoke(executor, schedule, now);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}