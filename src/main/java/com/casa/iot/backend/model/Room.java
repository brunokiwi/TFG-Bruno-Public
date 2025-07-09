package com.casa.iot.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Room {

    @Id 
    private String name;

    private boolean lightOn;
    private boolean detectOn;

    public Room() {}

    public Room(String name) {
        this.name = name;
        this.lightOn = false;
        this.detectOn = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isLightOn() { return lightOn; }
    public void setLightOn(boolean lightOn) { this.lightOn = lightOn; }

    public boolean isDetectOn() { return detectOn; }
    public void setDetectOn(boolean detectOn) { this.detectOn = detectOn; }
}
