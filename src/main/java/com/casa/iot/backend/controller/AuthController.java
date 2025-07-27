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
import com.casa.iot.backend.service.RFIDService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    private final EventLogService eventLogService;
    private final RFIDService rfidService;

    public AuthController(AuthService authService, EventLogService eventLogService, RFIDService rfidService ) {
        this.authService = authService;
        this.eventLogService = eventLogService;
        this.rfidService = rfidService;
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
    
    @PostMapping("/delete-user")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @RequestBody Map<String, String> requestData,
            HttpServletRequest request) {
        
        String usernameToDelete = requestData.get("usernameToDelete");
        String adminUsername = requestData.get("adminUsername");
        
        try {
            // Verificar que el usuario a eliminar existe
            if (!authService.userExists(usernameToDelete)) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Usuario no encontrado"
                ));
            }
            
            boolean deleted = authService.deleteUser(usernameToDelete);
            
            if (deleted) {
                // logging
                String ipAddress = getClientIpAddress(request);
                String details = String.format("{\"deletedUser\":\"%s\",\"deletedBy\":\"%s\",\"ipAddress\":\"%s\"}", 
                                              usernameToDelete, adminUsername, ipAddress);
                eventLogService.logSystemAction("USER_DELETED", null, details, "ADMIN_ACTION");
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuario eliminado exitosamente",
                    "deletedUser", usernameToDelete
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error al eliminar usuario"
                ));
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/rfid/register")
    public ResponseEntity<?> initiateRfidRegistration(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        if (username == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Falta el usuario"));
        }
        rfidService.startRfidRegistration(username);
        return ResponseEntity.ok(Map.of("success", true, "message", "Comando de inicio de registro RFID enviado"));
    }

    @PostMapping("/rfid/cancel")
    public ResponseEntity<?> cancelRfidRegistration(@RequestBody Map<String, String> data) {
        String username = data.get("username");
        if (username == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Falta el usuario"));
        }
        rfidService.cancelRfidRegistration(username);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/rfid/{username}")
    public ResponseEntity<?> getRfidUid(@PathVariable String username) {
        User user = authService.findByUsername(username);
        if (user == null || user.getRfidUid() == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Usuario no encontrado"));
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "rfidUid", user.getRfidUid() // puede ser null si no tiene
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
