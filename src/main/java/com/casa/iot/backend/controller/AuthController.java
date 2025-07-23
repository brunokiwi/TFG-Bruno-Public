package com.casa.iot.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.casa.iot.backend.model.User;
import com.casa.iot.backend.model.UserRole;
import com.casa.iot.backend.service.AuthService;
import com.casa.iot.backend.service.EventLogService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    private final EventLogService eventLogService;
    
    public AuthController(AuthService authService, EventLogService eventLogService) {
        this.authService = authService;
        this.eventLogService = eventLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpServletRequest request) {
        
        String username = credentials.get("username");
        String password = credentials.get("password");
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        User user = authService.authenticate(username, password);
        
        if (user != null) {
            // logging
            eventLogService.logLoginAttempt(username, true, ipAddress, userAgent);
            
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
            // logging TODO ip es overboard?
            eventLogService.logLoginAttempt(username, false, ipAddress, userAgent);
            
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Credenciales invalidos"
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> userData,
            HttpServletRequest request) {
        try {
            String username = userData.get("username");
            String password = userData.get("password");
            String roleStr = userData.getOrDefault("role", "USER");
            
            UserRole role = UserRole.valueOf(roleStr.toUpperCase());
            User user = authService.createUser(username, password, role);
            
            // logging
            String ipAddress = getClientIpAddress(request);
            eventLogService.logLoginAttempt(username, true, ipAddress, "REGISTRATION_SUCCESS");
            
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
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
