package com.casa.iot.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_room_name", columnList = "room_name"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String eventType; // USER_ACTION, SYSTEM_ACTION, MOVEMENT_DETECTED, SCHEDULE_EXECUTED, LOGIN_ATTEMPT

    @Column(nullable = false, length = 50)
    private String action; // LIGHT_ON, LIGHT_OFF, SENSOR_ON, SENSOR_OFF, SCHEDULE_CREATED, MOVEMENT, LOGIN_SUCCESS, LOGIN_FAILED

    @Column(name = "room_name", length = 255)
    private String roomName; // nullable para eventos de login

    @Column(name = "user_id", length = 255)
    private String userId; // null para eventos del sistema

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details; // JSON con información adicional

    @Column(length = 50)
    private String source; // USER, SCHEDULE, RFID, MOVEMENT_SENSOR, MANUAL, LOGIN

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // para login

    public Event() {}

    public Event(String eventType, String action, String roomName, String userId, String details, String source) {
        this.eventType = eventType;
        this.action = action;
        this.roomName = roomName;
        this.userId = userId;
        this.details = details;
        this.source = source;
        this.timestamp = LocalDateTime.now();
    }

    public static Event userAction(String action, String roomName, String userId, String details) {
        return new Event("USER_ACTION", action, roomName, userId, details, "USER");
    }

    public static Event systemAction(String action, String roomName, String details, String source) {
        return new Event("SYSTEM_ACTION", action, roomName, null, details, source);
    }

    public static Event movementDetected(String roomName, String details) {
        return new Event("MOVEMENT_DETECTED", "MOVEMENT", roomName, null, details, "MOVEMENT_SENSOR");
    }

    public static Event scheduleExecuted(String action, String roomName, String scheduleId) {
        return new Event("SCHEDULE_EXECUTED", action, roomName, null, 
                        "{\"scheduleId\":\"" + scheduleId + "\"}", "SCHEDULE");
    }

    public static Event loginAttempt(String username, boolean success, String ipAddress, String details) {
        Event event = new Event("LOGIN_ATTEMPT", 
                               success ? "LOGIN_SUCCESS" : "LOGIN_FAILED", 
                               null, username, details, "LOGIN");
        event.setIpAddress(ipAddress);
        return event;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}