package com.casa.iot.backend.service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.repository.RoomRepository;

@Service
public class VacationModeService {
    
    private final RoomRepository roomRepository;
    private final MovementService movementService;
    private final LightService lightService;
    private final AtomicBoolean vacationModeActive = new AtomicBoolean(false);
    private final Random random = new Random();
    
    public VacationModeService(RoomRepository roomRepository, MovementService movementService, LightService lightService) {
        this.roomRepository = roomRepository;
        this.movementService = movementService;
        this.lightService = lightService;
    }
    
    public void activateVacationMode() {
        vacationModeActive.set(true);
        activateAllMotionSensors();
    }
    
    public void deactivateVacationMode() {
        vacationModeActive.set(false);
    }
    
    public boolean isVacationModeActive() {
        return vacationModeActive.get();
    }
    
    private void activateAllMotionSensors() {
        List<Room> rooms = roomRepository.findAll();
        for (Room room : rooms) {
            movementService.sendAlarmCommand(room.getName(), true);
        }
    }
    
    @Scheduled(fixedRate = 60000)
    public void simulateRandomActivity() {
        if (!vacationModeActive.get()) return;
        
        List<Room> rooms = roomRepository.findAll();
        if (rooms.isEmpty()) return;
        
        Room randomRoom = rooms.get(random.nextInt(rooms.size()));
        boolean randomState = random.nextBoolean();
        
        lightService.sendLightCommand(randomRoom.getName(), randomState);
    }
}
