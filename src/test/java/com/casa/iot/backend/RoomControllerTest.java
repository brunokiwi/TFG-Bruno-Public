package com.casa.iot.backend;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.casa.iot.backend.model.Room;
import com.casa.iot.backend.repository.RoomRepository;

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepo;

    @BeforeEach
    void init() {
        roomRepo.deleteAll();
        roomRepo.save(new Room("kitchen"));
    }

    @Test
    void getAllAndByName() throws Exception {
        mockMvc.perform(get("/rooms"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(1)))
               .andExpect(jsonPath("$[0].name").value("kitchen"));

        mockMvc.perform(get("/rooms/kitchen"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("kitchen"));
    }

    @Test
    void lightAndAlarmEndpoints() throws Exception {
        mockMvc.perform(post("/rooms/kitchen/light").param("state","true"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/rooms/kitchen/alarm").param("state","false"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void removeRoomEndpoint() throws Exception {
        mockMvc.perform(post("/rooms/kitchen/remove"))
               .andExpect(status().isOk());

        mockMvc.perform(get("/rooms"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(0)));
    }
}