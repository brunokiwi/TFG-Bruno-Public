package com.casa.iot.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.casa.iot.backend.model.RoomSchedule;

public interface RoomScheduleRepository extends JpaRepository<RoomSchedule, Long> {
    List<RoomSchedule> findByRoom_Name(String roomName);

    @Query("SELECT s FROM RoomSchedule s WHERE FUNCTION('HOUR', s.time) = ?1 AND FUNCTION('MINUTE', s.time) = ?2")
    List<RoomSchedule> findByTimeHourAndTimeMinute(int hour, int minute);

    List<RoomSchedule> findByScheduleType(String scheduleType);
    
    @Query("SELECT s FROM RoomSchedule s WHERE s.scheduleType = ?1 AND FUNCTION('HOUR', s.time) = ?2 AND FUNCTION('MINUTE', s.time) = ?3")
    List<RoomSchedule> findByScheduleTypeAndTimeHourAndTimeMinute(String scheduleType, int hour, int minute);
}