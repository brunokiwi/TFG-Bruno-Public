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

    // cada 59 segundos revisa horarios
    @Scheduled(cron = "59 * * * * *")
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

    // Ver si estamos en el intervalo y si es necesario cambiar el estado del hardware
    private void executeIntervalSchedule(RoomSchedule schedule, LocalTime now) {
        LocalTime startTime = schedule.getStartTime();
        LocalTime endTime = schedule.getEndTime();
        boolean shouldBeOn = isNowInInterval(now, startTime, endTime);
        if (!shouldBeOn) {
            return;
        }
        String roomName = schedule.getRoomName();

        Room room = roomService.getRoomByName(roomName);
        if (room == null) return;

       
        boolean currentState = getCurrentState(room, schedule.getType());
        
        if (currentState != shouldBeOn) { // si no esta como debe
            execute(schedule, shouldBeOn);
        }

        // apagar al final del intervalo
        if (now.getHour() == endTime.getHour() && now.getMinute() == endTime.getMinute()) {
            // hay otro activo?
            boolean otherActive = scheduleService.getAllIntervalSchedules().stream()
                .filter(s -> !s.getId().equals(schedule.getId()))
                .filter(s -> s.getRoomName().equals(roomName))
                .filter(s -> s.getType().equals(schedule.getType()))
                .anyMatch(s -> isNowInInterval(now, s.getStartTime(), s.getEndTime()));

            if (!otherActive) {
                execute(schedule, false);
            }
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
        LocalTime nowTrunc = now.withSecond(0).withNano(0);
        LocalTime startTrunc = start.withSecond(0).withNano(0);
        LocalTime endTrunc = end.withSecond(0).withNano(0);

        if (startTrunc.isBefore(endTrunc) || startTrunc.equals(endTrunc)) {
            return (!nowTrunc.isBefore(startTrunc)) && (!nowTrunc.isAfter(endTrunc));
        } else {
            return (!nowTrunc.isBefore(startTrunc)) || (!nowTrunc.isAfter(endTrunc));
        }
    }
}