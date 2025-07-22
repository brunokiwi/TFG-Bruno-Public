package com.casa.iot.backend.service;

import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import jakarta.annotation.PostConstruct;


@Service
public class NotificationService {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId("intento-78792")
                    .build();

                FirebaseApp.initializeApp(options);
            }
        } catch (Exception e) {
            System.err.println("Error inicializando Firebase: " + e.getMessage());
        }
    }

    public void sendMovementAlert(String roomName) {
        try {
            Notification notification = Notification.builder()
                .setTitle("Movimiento detectado")
                .setBody("Se ha detectado movimiento en " + roomName)
                .build();

            Message message = Message.builder()
                .setNotification(notification)
                .setTopic("movement_alerts")
                .putData("roomName", roomName)
                .putData("type", "MOVEMENT_DETECTED")
                .build();
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Notificacion enviada: " + response);
            
        } catch (Exception e) {
            System.err.println("Error enviando notificacion: " + e.getMessage());
        }
    }
}