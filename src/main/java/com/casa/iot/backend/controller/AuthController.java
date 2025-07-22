package com.casa.iot.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.casa.iot.backend.model.User;
import com.casa.iot.backend.model.UserRole;
import com.casa.iot.backend.service.AuthService;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        User user = authService.authenticate(username, password);
        
        if (user != null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Correcto",
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole().toString()
                )
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Credenciales invalidos"
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> userData) {
        try {
            String username = userData.get("username");
            String password = userData.get("password");
            String roleStr = userData.getOrDefault("role", "USER");
            
            UserRole role = UserRole.valueOf(roleStr.toUpperCase());
            
            User user = authService.createUser(username, password, role);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuario creado con éxito",
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole().toString()
                )
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/validate/{username}")
    public ResponseEntity<Map<String, Object>> validateUser(@PathVariable String username) {
        boolean isAdmin = authService.isAdmin(username);
        return ResponseEntity.ok(Map.of(
            "username", username,
            "isAdmin", isAdmin
        ));
    }
}
