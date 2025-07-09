package com.casa.iot.backend.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.casa.iot.backend.model.RoomSchedule;

@Component
public class RoomScheduleExecutor {
    private final RoomScheduleService scheduleService;
    private final LightService lightService;
    private final MovementService movementService;

    public RoomScheduleExecutor(RoomScheduleService scheduleService, LightService lightService, MovementService movementService) {
        this.scheduleService = scheduleService;
        this.lightService = lightService;
        this.movementService = movementService;
    }

    // Ejecuta cada minuto
    @Scheduled(cron = "0 * * * * *")
    public void executeSchedules() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        // 1. Ejecuta programaciones puntuales
        List<RoomSchedule> punctual = scheduleService.getSchedulesForTime(now);
        for (RoomSchedule schedule : punctual) {
            if (schedule.getTime() != null && schedule.getTime().getHour() == now.getHour() && schedule.getTime().getMinute() == now.getMinute()) {
                execute(schedule);
            }
        }

        // 2. Ejecuta programaciones por intervalo
        List<RoomSchedule> all = scheduleService.getAllSchedules();
        for (RoomSchedule schedule : all) {
            if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                if (isNowInInterval(now, schedule.getStartTime(), schedule.getEndTime())) {
                    execute(schedule);
                }
            }
        }
    }

    private void execute(RoomSchedule schedule) {
        String roomName = schedule.getRoomName();
        boolean state = schedule.isState();
        if ("light".equals(schedule.getType())) {
            lightService.updateLight(roomName, state);
        } else if ("alarm".equals(schedule.getType())) {
            movementService.updateAlarm(roomName, state);
        }
    }

    private boolean isNowInInterval(LocalTime now, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        } else { // Intervalo que cruza medianoche
            return !now.isBefore(start) || now.isBefore(end);
        }
    }
}