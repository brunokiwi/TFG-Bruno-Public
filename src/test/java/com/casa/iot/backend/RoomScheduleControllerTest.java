package com.casa.iot.backend;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.repository.RoomRepository;
import com.casa.iot.backend.repository.RoomScheduleRepository;

@SpringBootTest
@AutoConfigureMockMvc
class RoomScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomScheduleRepository scheduleRepository;

    @BeforeEach
    void setup() {
        scheduleRepository.deleteAll();
        roomRepository.deleteAll();
        roomRepository.save(new Room("salon"));
    }

    @Test
    void testCreateAndGetSchedule() throws Exception {
        // Crear programación puntual
        mockMvc.perform(post("/rooms/salon/schedules")
                .param("type", "light")
                .param("state", "true")
                .param("time", "20:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("light"))
                .andExpect(jsonPath("$.state").value(true))
                .andExpect(jsonPath("$.time").value("20:00:00"));

        // Consultar programaciones
        mockMvc.perform(get("/rooms/salon/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("light"));
    }

    @Test
    void testCreateIntervalSchedule() throws Exception {
        mockMvc.perform(post("/rooms/salon/schedules")
                .param("type", "alarm")
                .param("state", "true")
                .param("startTime", "22:00")
                .param("endTime", "06:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("alarm"))
                .andExpect(jsonPath("$.startTime").value("22:00:00"))
                .andExpect(jsonPath("$.endTime").value("06:00:00"));
    }

    @Test
    void testDeleteSchedule() throws Exception {
        // Crear programación
        mockMvc.perform(post("/rooms/salon/schedules")
                .param("type", "light")
                .param("state", "true")
                .param("time", "21:00"))
                .andExpect(status().isOk());

        // Extraer el id (puedes usar una librería JSON para parsear si lo necesitas)
        Long id = scheduleRepository.findAll().get(0).getId();

        // Eliminar programación
        mockMvc.perform(delete("/rooms/salon/schedules/" + id))
                .andExpect(status().isOk());
    }
}
