package com.casa.iot.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.casa.iot.backend.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByRoomNameOrderByTimestampDesc(String roomName, Pageable pageable);
    Page<Event> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    Page<Event> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);
    Page<Event> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // TODO: revisar, necesitamos todas?
    @Query("SELECT e FROM Event e WHERE e.roomName = :roomName AND e.timestamp BETWEEN :start AND :end ORDER BY e.timestamp DESC")
    Page<Event> findByRoomAndDateRange(
        @Param("roomName") String roomName, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end, 
        Pageable pageable);
    
    @Query("SELECT e FROM Event e WHERE e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<Event> findRecentEvents(@Param("since") LocalDateTime since);
    
    @Query("SELECT e.eventType, COUNT(e) FROM Event e WHERE e.timestamp >= :since GROUP BY e.eventType")
    List<Object[]> countEventsByTypeSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM Event e WHERE e.eventType = 'LOGIN_ATTEMPT' AND e.action = 'LOGIN_FAILED' AND e.userId = :username AND e.timestamp >= :since ORDER BY e.timestamp DESC")
    List<Event> findFailedLoginAttempts(@Param("username") String username, @Param("since") LocalDateTime since);
    
    // Borrar
    @Modifying
    @Query("DELETE FROM Event e WHERE e.timestamp < :cutoffDate")
    int deleteEventsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // num de eventos en las ultimas 24 horas por usuario
    @Query("SELECT COUNT(e) FROM Event e WHERE e.userId = :userId AND e.timestamp >= :since")
    long countUserEventsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);
}
