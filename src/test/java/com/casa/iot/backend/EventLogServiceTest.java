package com.casa.iot.backend;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.model.Event;
import com.casa.iot.backend.repository.EventRepository;
import com.casa.iot.backend.service.EventLogService;

class EventLogServiceTest {

    private EventRepository eventRepository;
    private EventLogService eventLogService;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventLogService = new EventLogService(eventRepository);
    }

    @Test
    void getFailedLoginAttemptsReturnsList() {
        Event event = Event.loginAttempt("user", false, "127.0.0.1", "{}");
        when(eventRepository.findFailedLoginAttempts(eq("user"), any(LocalDateTime.class)))
                .thenReturn(List.of(event));
        List<Event> result = eventLogService.getFailedLoginAttempts("user", 2);
        assertEquals(1, result.size());
        assertEquals("user", result.get(0).getUserId());
    }

    @Test
    void getEventStatisticsReturnsList() {
        when(eventRepository.countEventsByTypeSince(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(new Object[]{"LOGIN", 5L}));
        List<Object[]> stats = eventLogService.getEventStatistics(7);
        assertEquals(1, stats.size());
        assertEquals("LOGIN", stats.get(0)[0]);
    }

    @Test
    void logLoginAttemptSavesEvent() {
        eventLogService.logLoginAttempt("user", true, "127.0.0.1", "agent");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void getRecentEventsReturnsList() {
        Event e = mock(Event.class);
        when(eventRepository.findRecentEvents(any())).thenReturn(List.of(e));
        List<Event> result = eventLogService.getRecentEvents(5);
        assertEquals(1, result.size());
    }

    @Test
    void cleanupOldEventsDeletesAndLogs() {
        when(eventRepository.deleteEventsOlderThan(any())).thenReturn(2);
        when(eventRepository.save(any())).thenReturn(mock(Event.class));
        eventLogService.cleanupOldEvents();
        verify(eventRepository).deleteEventsOlderThan(any());
        verify(eventRepository).save(any());
    }

    @Test
    void testLogMovementDetected() {
        when(eventRepository.save(any(Event.class))).thenReturn(mock(Event.class));
        CompletableFuture<Void> future = eventLogService.logMovementDetected("salon", "{\"sensorType\":\"PIR\"}");
        future.join();
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testLogScheduleExecution() {
        when(eventRepository.save(any(Event.class))).thenReturn(mock(Event.class));
        CompletableFuture<Void> future = eventLogService.logScheduleExecution("LIGHT_ON", "salon", "123");
        future.join(); 
        verify(eventRepository).save(any(Event.class));
    }
}