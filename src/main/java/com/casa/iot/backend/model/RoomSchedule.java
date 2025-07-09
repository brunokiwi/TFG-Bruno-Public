package com.casa.iot.backend.model;

import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class RoomSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_name", referencedColumnName = "name")
    private Room room;

    private String type; // "light" o "alarm"
    private boolean state; // true=ON, false=OFF

    private LocalTime time;      // Para ejecución puntual (opcional)
    private LocalTime startTime; // Para intervalos (opcional)
    private LocalTime endTime;   // Para intervalos (opcional)

    public RoomSchedule() {}

    public RoomSchedule(Room room, String type, boolean state, LocalTime time) {
        this.room = room;
        this.type = type;  
        this.state = state; 
        this.time = time;
    }

    public RoomSchedule(Room room, String type, boolean state, LocalTime startTime, LocalTime endTime) {
        this.room = room;
        this.type = type;
        this.state = state;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getRoomName() {
        return room != null ? room.getName() : null;
    }

    public String getType() {
        return type;
    }

    public boolean isState() {
        return state;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}