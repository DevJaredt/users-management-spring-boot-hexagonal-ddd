package com.jcaa.notifiers.infrastructure.adapter.kafka;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jcaa.notifiers.application.exception.EmailDeliveryException;
import com.jcaa.notifiers.application.service.UserNotificationService;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.NotificationStatusMessage.NotificationStatus;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.UserEventMessage;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.UserEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserEventListener")
@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

  private static final String EVENT_ID = "evt-1";
  private static final String USER_ID = "user-1";
  private static final String NAME = "Jared";
  private static final String EMAIL = "jared@example.com";
  private static final String ROLE = "ADMIN";
  private static final String STATUS = "ACTIVE";

  @Mock private UserNotificationService userNotificationService;
  @Mock private NotificationStatusPublisher statusPublisher;

  private UserEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new UserEventListener(userNotificationService, statusPublisher);
  }

  @Test
  @DisplayName("un USER_CREATED notifica y publica el estado SENT")
  void shouldNotifyAndPublishSentOnCreated() {
    // Act
    listener.onUserEvent(event(UserEventType.USER_CREATED));

    // Assert
    verify(userNotificationService).notifyUserCreated(NAME, EMAIL, ROLE);
    assertEquals(NotificationStatus.SENT, captureStatus());
  }

  @Test
  @DisplayName("un USER_UPDATED notifica con el estado de la cuenta")
  void shouldNotifyOnUpdated() {
    // Act
    listener.onUserEvent(event(UserEventType.USER_UPDATED));

    // Assert
    verify(userNotificationService).notifyUserUpdated(NAME, EMAIL, ROLE, STATUS);
    assertEquals(NotificationStatus.SENT, captureStatus());
  }

  @Test
  @DisplayName("un USER_DELETED no envia correo pero publica SKIPPED")
  void shouldSkipDeletedEvent() {
    // Act
    listener.onUserEvent(event(UserEventType.USER_DELETED));

    // Assert
    verify(userNotificationService, never()).notifyUserCreated(anyString(), anyString(), anyString());
    assertEquals(NotificationStatus.SKIPPED, captureStatus());
  }

  @Test
  @DisplayName("si el proveedor falla publica FAILED sin propagar la excepcion de vuelta a Kafka")
  void shouldPublishFailedWhenDeliveryFails() {
    // Arrange
    doThrow(EmailDeliveryException.becauseProviderFailed("HTTP 500"))
        .when(userNotificationService)
        .notifyUserCreated(NAME, EMAIL, ROLE);

    // Act & Assert
    assertDoesNotThrow(() -> listener.onUserEvent(event(UserEventType.USER_CREATED)));
    assertEquals(NotificationStatus.FAILED, captureStatus());
  }

  @Test
  @DisplayName("un evento sin email se marca FAILED sin llamar al proveedor")
  void shouldFailWhenEmailIsMissing() {
    // Arrange
    final UserEventMessage incomplete =
        new UserEventMessage(
            EVENT_ID, UserEventType.USER_CREATED, "2026-01-01T00:00:00Z", USER_ID, NAME, "  ", ROLE, STATUS);

    // Act
    listener.onUserEvent(incomplete);

    // Assert
    verify(userNotificationService, never()).notifyUserCreated(anyString(), anyString(), anyString());
    assertEquals(NotificationStatus.FAILED, captureStatus());
  }

  private NotificationStatus captureStatus() {
    final ArgumentCaptor<NotificationStatus> captor = ArgumentCaptor.forClass(NotificationStatus.class);
    verify(statusPublisher).publish(any(UserEventMessage.class), captor.capture(), any());
    return captor.getValue();
  }

  private static UserEventMessage event(final UserEventType type) {
    return new UserEventMessage(
        EVENT_ID, type, "2026-01-01T00:00:00Z", USER_ID, NAME, EMAIL, ROLE, STATUS);
  }
}
