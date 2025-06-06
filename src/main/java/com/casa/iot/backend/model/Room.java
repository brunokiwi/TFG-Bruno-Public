package com.casa.iot.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private boolean luzEncendida;
    private boolean movimientoDetectado;

    // Constructores
    public Room() {}

    public Room(String nombre) {
        this.nombre = nombre;
        this.luzEncendida = false;
        this.movimientoDetectado = false;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isLuzEncendida() {
        return luzEncendida;
    }

    public void setLuzEncendida(boolean luzEncendida) {
        this.luzEncendida = luzEncendida;
    }

    public boolean isMovimientoDetectado() {
        return movimientoDetectado;
    }

    public void setMovimientoDetectado(boolean movimientoDetectado) {
        this.movimientoDetectado = movimientoDetectado;
    }
}
