package com.casa.iot.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getFailedLoginsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events/failed-logins/usuario"))
                .andExpect(status().isOk());
    }

    @Test
    void manualCleanupShouldReturnOk() throws Exception {
        mockMvc.perform(post("/events/cleanup"))
                .andExpect(status().isOk());
    }
}