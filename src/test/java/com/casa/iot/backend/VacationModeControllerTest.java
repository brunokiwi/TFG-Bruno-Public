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
class VacationModeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void activateThenStatus() throws Exception {
        mockMvc.perform(post("/vacation-mode/activate"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("Modo vacaciones activado"))
               .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/vacation-mode/status"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void deactivateThenStatus() throws Exception {
        mockMvc.perform(post("/vacation-mode/deactivate"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("Modo vacaciones desactivado"))
               .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/vacation-mode/status"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void activateAndDeactivateVacationMode() throws Exception {
        mockMvc.perform(post("/vacation-mode/activate"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/vacation-mode/deactivate"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.active").value(false));
    }
}