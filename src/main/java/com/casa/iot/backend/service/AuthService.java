package com.casa.iot.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.casa.iot.backend.model.User;
import com.casa.iot.backend.model.UserRole;
import com.casa.iot.backend.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostConstruct
    public void createDefaultAdmin() {
        // crea adminsi no existe
        if (!userRepository.existsByUsername("admin")) {
            String hashedPassword = passwordEncoder.encode("admin123");
            User admin = new User("admin", hashedPassword, UserRole.ADMIN);
            userRepository.save(admin);
            System.out.println("admin creado - username: admin, password: admin123");
        }
        
        // crea usuario normal si no existe  
        if (!userRepository.existsByUsername("usuario")) {
            String hashedPassword = passwordEncoder.encode("user123");
            User normalUser = new User("usuario", hashedPassword, UserRole.USER);
            userRepository.save(normalUser);
            System.out.println("usuario  creado - username: usuario, password: user123");
        }
    }

    public User authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                return user;
            }
        }
        return null;
    }

    public User createUser(String username, String password, UserRole role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("El usuario ya existe");
        }
        
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username, hashedPassword, role);
        return userRepository.save(user);
    }

    public boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }
}
