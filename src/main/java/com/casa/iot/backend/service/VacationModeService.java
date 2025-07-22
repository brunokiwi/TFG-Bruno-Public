package com.casa.iot.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private List<String> roomNamesCache = new ArrayList<>();
    
    public VacationModeService(RoomRepository roomRepository, MovementService movementService, LightService lightService) {
        this.roomRepository = roomRepository;
        this.movementService = movementService;
        this.lightService = lightService;
    }
    
    public void activateVacationMode() {
        vacationModeActive.set(true);
        roomNamesCache = roomRepository.findAll()
                                       .stream()
                                       .map(Room::getName)
                                       .collect(Collectors.toList());
        activateAllMotionSensors();
    }
    
    public void deactivateVacationMode() {
        vacationModeActive.set(false);
        roomNamesCache.clear();  // limpiar cache
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
        if (!vacationModeActive.get() || roomNamesCache.isEmpty()) {
            return;
        }
        // cogemos habitacion del cache para ahorrar accesos a bd
        String randomRoom = roomNamesCache.get(random.nextInt(roomNamesCache.size()));
        boolean randomState = random.nextBoolean();
        lightService.sendLightCommand(randomRoom, randomState);
    }
}
