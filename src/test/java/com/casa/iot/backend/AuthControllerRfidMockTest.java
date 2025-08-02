package com.casa.iot.backend;

import java.util.Optional;

import com.casa.iot.backend.model.User;
import com.casa.iot.backend.model.UserRole;
import com.casa.iot.backend.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerRfidMockTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void testGetRfidUidSuccess() throws Exception {
        User usuario = new User("uzer", "pass", UserRole.USER);
        usuario.setRfidUid("RFID1234");
        when(userRepository.findByUsername("uzer")).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/auth/rfid/uzer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.rfidUid").value("RFID1234"));
    }
}