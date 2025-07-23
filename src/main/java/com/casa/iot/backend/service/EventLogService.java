package com.casa.iot.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casa.iot.backend.model.Event;
import com.casa.iot.backend.repository.EventRepository;

@Service
public class EventLogService {
    
    private final EventRepository eventRepository;
    
    public EventLogService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    
    // loging asincrono
    @Async
    public CompletableFuture<Void> logUserAction(String action, String roomName, String userId, String details) {
        try {
            Event event = Event.userAction(action, roomName, userId, details);
            eventRepository.save(event);
            System.out.println("🔷 USER ACTION: " + userId + " - " + action + " en " + roomName);
        } catch (Exception e) {
            System.err.println("Error logging user action: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Async
    public CompletableFuture<Void> logSystemAction(String action, String roomName, String details, String source) {
        try {
            Event event = Event.systemAction(action, roomName, details, source);
            eventRepository.save(event);
            System.out.println("⚙️ SYSTEM ACTION: " + action + " en " + roomName + " (" + source + ")");
        } catch (Exception e) {
            System.err.println("Error logging system action: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Async
    public CompletableFuture<Void> logMovementDetected(String roomName, String sensorDetails) {
        try {
            Event event = Event.movementDetected(roomName, sensorDetails);
            eventRepository.save(event);
            System.out.println("MOVEMENT: Detectado en " + roomName);
        } catch (Exception e) {
            System.err.println("Error logging movement: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Async
    public CompletableFuture<Void> logScheduleExecution(String action, String roomName, String scheduleId) {
        try {
            Event event = Event.scheduleExecuted(action, roomName, scheduleId);
            eventRepository.save(event);
            System.out.println("SCHEDULE: " + action + " en " + roomName + " (ID: " + scheduleId + ")");
        } catch (Exception e) {
            System.err.println("Error logging schedule execution: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    // Logging síncrono para login (necesita ser inmediato para seguridad)
    public void logLoginAttempt(String username, boolean success, String ipAddress, String userAgent) {
        try {
            String details = String.format("{\"userAgent\":\"%s\",\"timestamp\":\"%s\"}", 
                                         userAgent != null ? userAgent : "unknown", 
                                         LocalDateTime.now());
            Event event = Event.loginAttempt(username, success, ipAddress, details);
            eventRepository.save(event);
            
            if (success) {
                System.out.println("LOGIN SUCCESS: " + username + " desde " + ipAddress);
            } else {
                System.out.println("LOGIN FAILED: " + username + " desde " + ipAddress);
            }
        } catch (Exception e) {
            System.err.println("Error logging login attempt: " + e.getMessage());
        }
    }
    
    // Métodos de consulta
    public List<Event> getRecentEvents(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return eventRepository.findRecentEvents(since);
    }
    
    public Page<Event> getEventsByDateRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
    }
    
    public Page<Event> getRoomEvents(String roomName, LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByRoomAndDateRange(roomName, start, end, pageable);
    }
    
    public Page<Event> getUserEvents(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }
    
    public List<Event> getFailedLoginAttempts(String username, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return eventRepository.findFailedLoginAttempts(username, since);
    }
    
    public List<Object[]> getEventStatistics(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return eventRepository.countEventsByTypeSince(since);
    }
    
    // limpieza automatica a las 2am cada dia 
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldEvents() {
        try {
            // borrar eventos de hace mas de 30 dias
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            int deletedCount = eventRepository.deleteEventsOlderThan(cutoffDate);
            
            if (deletedCount > 0) {
                System.out.println("🧹 CLEANUP: Eliminados " + deletedCount + " eventos antiguos");
                
                // logear limpieza
                Event cleanupEvent = Event.systemAction("CLEANUP_COMPLETED", null, 
                    "{\"deletedEvents\":" + deletedCount + ",\"cutoffDate\":\"" + cutoffDate + "\"}", 
                    "SYSTEM");
                eventRepository.save(cleanupEvent);
            }
        } catch (Exception e) {
            System.err.println("Error durante limpieza de logs: " + e.getMessage());
        }
    }
    
}