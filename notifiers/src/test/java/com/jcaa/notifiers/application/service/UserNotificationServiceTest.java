package com.jcaa.notifiers.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.jcaa.notifiers.application.port.out.EmailSenderPort;
import com.jcaa.notifiers.application.port.out.dto.EmailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserNotificationService")
@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

  private static final String NAME = "Jared Trujillo";
  private static final String EMAIL = "jared@example.com";
  private static final String ROLE = "ADMIN";
  private static final String STATUS = "ACTIVE";

  @Mock private EmailSenderPort emailSenderPort;

  private UserNotificationService service;

  @BeforeEach
  void setUp() {
    service = new UserNotificationService(emailSenderPort);
  }

  @Test
  @DisplayName("notifyAccountCreated() compone el mensaje de alta: usuario y cuenta asociada")
  void shouldComposeAccountCreatedMessage() {
    // Act
    service.notifyAccountCreated(NAME, EMAIL);

    // Assert
    final EmailMessage message = captureMessage();
    assertAll(
        "mensaje de alta",
        () -> assertEquals(EMAIL, message.recipientEmail()),
        () -> assertEquals(NAME, message.recipientName()),
        () -> assertTrue(message.subject().contains("creada")),
        () -> assertTrue(message.htmlBody().contains(NAME)),
        () -> assertTrue(message.htmlBody().contains(EMAIL)),
        () -> assertTrue(message.htmlBody().contains("usuario")),
        () -> assertTrue(message.htmlBody().contains("cuenta")));
  }

  @Test
  @DisplayName("notifyUserUpdated() incluye el estado de la cuenta")
  void shouldComposeUpdatedMessageWithStatus() {
    // Act
    service.notifyUserUpdated(NAME, EMAIL, ROLE, STATUS);

    // Assert
    final EmailMessage message = captureMessage();
    assertAll(
        "mensaje de actualizacion",
        () -> assertTrue(message.subject().contains("actualizada")),
        () -> assertTrue(message.htmlBody().contains(STATUS)));
  }

  @Test
  @DisplayName("escapa el HTML de los valores del evento para evitar inyeccion")
  void shouldEscapeHtmlFromEventValues() {
    // Act
    service.notifyAccountCreated("<script>alert(1)</script>", EMAIL);

    // Assert
    final EmailMessage message = captureMessage();
    assertAll(
        "escape de HTML",
        () -> assertFalse(message.htmlBody().contains("<script>")),
        () -> assertTrue(message.htmlBody().contains("&lt;script&gt;")));
  }

  private EmailMessage captureMessage() {
    final ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
    verify(emailSenderPort).send(captor.capture());
    return captor.getValue();
  }
}
