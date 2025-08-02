package com.casa.iot.backend;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.casa.iot.backend.model.User;
import com.casa.iot.backend.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testLoginFail() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"wrong\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRfidRegisterBadRequest() throws Exception {
        mockMvc.perform(post("/auth/rfid/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"badkey\":\"value\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginSuccess() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"usuario\",\"password\":\"user123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.username").value("usuario"));
    }

    @Test
    void testRegisterUserSuccess() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nuevo\",\"password\":\"nuevo123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.username").value("nuevo"));  
        Optional<User> user = userRepository.findByUsername("nuevo");
        user.ifPresent(userRepository::delete);
    }

    @Test
    void testRegisterUserAlreadyExists() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testValidateUserAdmin() throws Exception {
        mockMvc.perform(get("/auth/validate/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAdmin").value(true));
    }

    @Test
    void testValidateUserNotAdmin() throws Exception {
        mockMvc.perform(get("/auth/validate/usuario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAdmin").value(false));
    }

    @Test
    void testInitiateRfidRegistrationBadRequest() throws Exception {
        mockMvc.perform(post("/auth/rfid/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInitiateRfidRegistrationOk() throws Exception {
        mockMvc.perform(post("/auth/rfid/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"usuario\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testCancelRfidRegistrationBadRequest() throws Exception {
        mockMvc.perform(post("/auth/rfid/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCancelRfidRegistrationOk() throws Exception {
        mockMvc.perform(post("/auth/rfid/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"usuario\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetRfidUidNotFound() throws Exception {
        mockMvc.perform(get("/auth/rfid/noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testDeleteUserNotFound() throws Exception {
        mockMvc.perform(post("/auth/delete-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameToDelete\":\"noexiste\",\"adminUsername\":\"admin\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testDeleteUserSuccess() throws Exception {
        mockMvc.perform(post("/auth/delete-user")
                .contentType("application/json")
                .content("{\"usernameToDelete\":\"usuario\",\"adminUsername\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedUser").value("usuario"));
    }

}