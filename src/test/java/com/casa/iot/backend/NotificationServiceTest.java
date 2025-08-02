package com.casa.iot.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casa.iot.backend.service.NotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

class NotificationServiceTest {

    NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
    }

    @Test
    void testSendMovementAlert() throws Exception {
        try (MockedStatic<FirebaseMessaging> firebaseMessagingMockedStatic = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            firebaseMessagingMockedStatic.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            when(firebaseMessaging.send(any(Message.class))).thenReturn("mocked-response");

            notificationService.sendMovementAlert("salon");

            verify(firebaseMessaging).send(any(Message.class));
        }
    }
}