package com.casa.iot.backend.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.model.RoomSchedule;

@Component
public class RoomScheduleExecutor {
    private final RoomScheduleService scheduleService;
    private final LightService lightService;
    private final MovementService movementService;
    private final RoomService roomService;

    public RoomScheduleExecutor(RoomScheduleService scheduleService, LightService lightService, MovementService movementService, RoomService roomService) {
        this.scheduleService = scheduleService;
        this.lightService = lightService;
        this.movementService = movementService;
        this.roomService = roomService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void executeSchedules() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        // horarios puntuales
        List<RoomSchedule> punctual = scheduleService.getPunctualSchedulesForTime(now);
        for (RoomSchedule schedule : punctual) {
            if (schedule.getTime() != null && schedule.getTime().getHour() == now.getHour() && schedule.getTime().getMinute() == now.getMinute()) {
                execute(schedule);
            }
        }

        // horarios de intervalo
        List<RoomSchedule> intervalSchedules = scheduleService.getAllIntervalSchedules();
        for (RoomSchedule schedule : intervalSchedules) {
            executeIntervalSchedule(schedule, now);
        }
    }

    private void executeIntervalSchedule(RoomSchedule schedule, LocalTime now) {
        String roomName = schedule.getRoomName();
        Room room = roomService.getRoomByName(roomName);
        
        if (room == null) return;
        
        // Ver si estamos en el intervalo y si es necesario cambiar el estado del hardware 
        boolean shouldBeActive = isNowInInterval(now, schedule.getStartTime(), schedule.getEndTime());
        boolean currentState = getCurrentState(room, schedule.getType());
        boolean targetState = shouldBeActive ? schedule.isState() : !schedule.isState();
        
        if (currentState != targetState) {
            execute(schedule, targetState);
        }
    }

    private boolean getCurrentState(Room room, String type) {
        if ("light".equals(type)) {
            return room.isLightOn();
        } else if ("alarm".equals(type)) {
            return room.isDetectOn();
        }
        return false;
    }

    private void execute(RoomSchedule schedule) {
        execute(schedule, schedule.isState());
    }

    private void execute(RoomSchedule schedule, boolean targetState) {
        String roomName = schedule.getRoomName();
        if ("light".equals(schedule.getType())) {
            lightService.updateLight(roomName, targetState);
        } else if ("alarm".equals(schedule.getType())) {
            movementService.updateAlarm(roomName, targetState);
        }
    }

    private boolean isNowInInterval(LocalTime now, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        } else {
            return !now.isBefore(start) || now.isBefore(end);
        }
    }
}