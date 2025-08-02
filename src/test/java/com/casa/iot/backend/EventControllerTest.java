package com.casa.iot.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRecentEventsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events/recent"))
                .andExpect(status().isOk());
    }

    @Test
    void getEventsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk());
    }

    @Test
    void getEventsWithParamsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events")
                .param("start", "2024-01-01T00:00:00")
                .param("end", "2025-01-01T00:00:00")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getRoomEventsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events/room/salon"))
                .andExpect(status().isOk());
    }

    @Test
    void getRoomEventsWithParamsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events/room/salon")
                .param("start", "2024-01-01T00:00:00")
                .param("end", "2025-01-01T00:00:00")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserEventsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events/user/usuario"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserEventsWithParamsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events/user/usuario")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getFailedLoginsShouldReturnOk() throws Exception {
        mockMvc.perform(get("/events/failed-logins/usuario"))
                .andExpect(status().isOk());
    }

    @Test
    void getFailedLoginsWithHoursParam() throws Exception {
        mockMvc.perform(get("/events/failed-logins/usuario")
                .param("hours", "12"))
                .andExpect(status().isOk());
    }

    @Test
    void manualCleanupShouldReturnOk() throws Exception {
        mockMvc.perform(post("/events/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.current_retention_days").exists())
                .andExpect(jsonPath("$.next_cleanup").exists());
    }
}