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
    private final EventLogService eventLogService;

    public RoomScheduleExecutor(RoomScheduleService scheduleService, LightService lightService, 
                               MovementService movementService, RoomService roomService, 
                               EventLogService eventLogService) {
        this.scheduleService = scheduleService;
        this.lightService = lightService;
        this.movementService = movementService;
        this.roomService = roomService;
        this.eventLogService = eventLogService;
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
        boolean shouldBeOn = isNowInInterval(now, schedule.getStartTime(), schedule.getEndTime());
        boolean currentState = getCurrentState(room, schedule.getType());
        
        if (currentState != shouldBeOn) { // si no esta como debe
            execute(schedule, shouldBeOn);
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

    private void execute(RoomSchedule schedule) { // solo puntuales
        execute(schedule, schedule.getState());
    }

    private void execute(RoomSchedule schedule, boolean targetState) {
        String roomName = schedule.getRoomName();
        if ("light".equals(schedule.getType())) {
            lightService.sendLightCommand(roomName, targetState);
        } else if ("alarm".equals(schedule.getType())) {
            movementService.sendAlarmCommand(roomName, targetState);
        }
        
        // logging
        String action = targetState ? 
            (schedule.getType().equals("light") ? "LIGHT_ON" : "SENSOR_ON") :
            (schedule.getType().equals("light") ? "LIGHT_OFF" : "SENSOR_OFF");
        eventLogService.logScheduleExecution(action, roomName, schedule.getId().toString());
    }

    private boolean isNowInInterval(LocalTime now, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        } else {
            return !now.isBefore(start) || now.isBefore(end);
        }
    }
}