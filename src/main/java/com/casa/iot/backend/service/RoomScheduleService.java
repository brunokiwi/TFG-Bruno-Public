package com.casa.iot.backend.service;

import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.RoomSchedule;
import com.casa.iot.backend.repository.RoomScheduleRepository;

@Service
public class RoomScheduleService {
    private final RoomScheduleRepository scheduleRepository;

    public RoomScheduleService(RoomScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public RoomSchedule createSchedule(RoomSchedule schedule) {
        return scheduleRepository.save(schedule);
    }

    public List<RoomSchedule> getSchedulesForRoom(String roomName) {
        return scheduleRepository.findByRoom_Name(roomName);
    }

    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }

    public List<RoomSchedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public List<RoomSchedule> getPunctualSchedulesForTime(LocalTime time) {
        return scheduleRepository.findByScheduleTypeAndTimeHourAndTimeMinute("puntual", time.getHour(), time.getMinute());
    }

    public List<RoomSchedule> getAllIntervalSchedules() {
        return scheduleRepository.findByScheduleType("intervalo");
    }
}