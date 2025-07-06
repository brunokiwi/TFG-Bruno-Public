package com.casa.iot.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Room {

    @Id
    private String name;

    private boolean lightOn;
    private boolean alarmOn;

    public Room() {}

    public Room(String name) {
        this.name = name;
        this.lightOn = false;
        this.alarmOn = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isLightOn() { return lightOn; }
    public void setLightOn(boolean lightOn) { this.lightOn = lightOn; }

    public boolean isAlarmOn() { return alarmOn; }
    public void setAlarmOn(boolean alarmOn) { this.alarmOn = alarmOn; }
}
