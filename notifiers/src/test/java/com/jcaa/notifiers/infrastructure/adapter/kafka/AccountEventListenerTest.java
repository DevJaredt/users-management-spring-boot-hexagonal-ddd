package com.jcaa.notifiers.infrastructure.adapter.kafka;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jcaa.notifiers.application.exception.EmailDeliveryException;
import com.jcaa.notifiers.application.service.UserNotificationService;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.AccountEventMessage;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.NotificationStatusMessage.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AccountEventListener")
@ExtendWith(MockitoExtension.class)
class AccountEventListenerTest {

  private static final String EVENT_ID = "evt-acc-1";
  private static final String ACCOUNT_ID = "acc-1";
  private static final String USER_ID = "user-1";
  private static final String NAME = "Jared";
  private static final String EMAIL = "jared@example.com";

  @Mock private UserNotificationService userNotificationService;
  @Mock private NotificationStatusPublisher statusPublisher;

  private AccountEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new AccountEventListener(userNotificationService, statusPublisher);
  }

  @Test
  @DisplayName("un ACCOUNT_CREATED notifica el alta (usuario + cuenta) y publica SENT")
  void shouldNotifyAndPublishSent() {
    // Act
    listener.onAccountEvent(event(EMAIL));

    // Assert
    verify(userNotificationService).notifyAccountCreated(NAME, EMAIL);
    assertEquals(NotificationStatus.SENT, captureStatus());
  }

  @Test
  @DisplayName("si el proveedor falla publica FAILED sin propagar la excepcion a Kafka")
  void shouldPublishFailedWhenDeliveryFails() {
    // Arrange
    doThrow(EmailDeliveryException.becauseProviderFailed("SMTP 550"))
        .when(userNotificationService)
        .notifyAccountCreated(NAME, EMAIL);

    // Act & Assert
    assertDoesNotThrow(() -> listener.onAccountEvent(event(EMAIL)));
    assertEquals(NotificationStatus.FAILED, captureStatus());
  }

  @Test
  @DisplayName("un evento sin email se marca FAILED sin llamar al proveedor")
  void shouldFailWhenEmailIsMissing() {
    // Act
    listener.onAccountEvent(event("   "));

    // Assert
    verify(userNotificationService, never()).notifyAccountCreated(anyString(), anyString());
    assertEquals(NotificationStatus.FAILED, captureStatus());
  }

  private NotificationStatus captureStatus() {
    final ArgumentCaptor<NotificationStatus> captor = ArgumentCaptor.forClass(NotificationStatus.class);
    verify(statusPublisher).publish(anyString(), anyString(), captor.capture(), any());
    return captor.getValue();
  }

  private static AccountEventMessage event(final String email) {
    return new AccountEventMessage(
        EVENT_ID, "ACCOUNT_CREATED", "2026-01-01T00:00:00Z", ACCOUNT_ID, USER_ID, NAME, email);
  }
}
