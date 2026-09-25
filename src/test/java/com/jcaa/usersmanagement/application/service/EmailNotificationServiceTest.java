package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.application.port.out.dto.EmailNotificationRequest;
import com.jcaa.usersmanagement.application.port.out.dto.EmailTemplate;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests para EmailNotificationService.
 *
 * <p>Cubre: composicion de la solicitud de notificacion (destinatario, asunto, plantilla y
 * variables), que la contrasena nunca viaja en el correo, y que el fallo del puerto SMTP se
 * registre sin propagarse.
 */
@DisplayName("EmailNotificationService")
@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

  @Mock private EmailSenderPort emailSenderPort;

  private EmailNotificationService service;

  private static final String EMAIL = "john@example.com";
  private static final String NAME = "John Arrieta";
  private static final String PASSWORD = "SecurePass1";
  private static final String TOKEN_NAME = "name";
  private static final String TOKEN_EMAIL = "email";
  private static final String TOKEN_ROLE = "role";
  private static final String TOKEN_STATUS = "status";
  private static final String TOKEN_PASSWORD = "password";

  private UserModel user;

  @BeforeEach
  void setUp() {
    service = new EmailNotificationService(emailSenderPort);
    user =
        new UserModel(
            new UserId("u-001"),
            new UserName(NAME),
            new UserEmail(EMAIL),
            UserPassword.fromPlainText(PASSWORD),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
  }

  // ── notifyUserCreated() — composicion de la solicitud

  @Test
  @DisplayName("notifyUserCreated() envia la plantilla USER_CREATED sin exponer la contrasena")
  void shouldSendCreatedNotificationWithoutPassword() {
    // Act
    service.notifyUserCreated(user);

    // Assert
    final EmailNotificationRequest request = captureRequest();
    assertAll(
        "solicitud de creacion",
        () -> assertEquals(EMAIL, request.recipientEmail()),
        () -> assertEquals(NAME, request.recipientName()),
        () -> assertEquals(EmailTemplate.USER_CREATED, request.template()),
        () -> assertTrue(request.subject().contains("creada"), "asunto de cuenta creada"),
        () -> assertEquals(NAME, request.variables().get(TOKEN_NAME)),
        () -> assertEquals(EMAIL, request.variables().get(TOKEN_EMAIL)),
        () -> assertEquals(UserRole.ADMIN.name(), request.variables().get(TOKEN_ROLE)),
        () -> assertFalse(request.variables().containsKey(TOKEN_PASSWORD), "no debe haber token de contrasena"),
        () -> assertFalse(request.variables().containsValue(PASSWORD), "la contrasena no debe viajar"));
  }

  // ── notifyUserUpdated() — composicion de la solicitud

  @Test
  @DisplayName("notifyUserUpdated() envia la plantilla USER_UPDATED con rol y estado")
  void shouldSendUpdatedNotificationWithRoleAndStatus() {
    // Act
    service.notifyUserUpdated(user);

    // Assert
    final EmailNotificationRequest request = captureRequest();
    assertAll(
        "solicitud de actualizacion",
        () -> assertEquals(EMAIL, request.recipientEmail()),
        () -> assertEquals(NAME, request.recipientName()),
        () -> assertEquals(EmailTemplate.USER_UPDATED, request.template()),
        () -> assertTrue(request.subject().contains("actualizada"), "asunto de cuenta actualizada"),
        () -> assertEquals(NAME, request.variables().get(TOKEN_NAME)),
        () -> assertEquals(EMAIL, request.variables().get(TOKEN_EMAIL)),
        () -> assertEquals(UserRole.ADMIN.name(), request.variables().get(TOKEN_ROLE)),
        () -> assertEquals(UserStatus.ACTIVE.name(), request.variables().get(TOKEN_STATUS)));
  }

  // ── fallo SMTP no propagado

  @Test
  @DisplayName("notifyUserCreated() no propaga el fallo del puerto SMTP")
  void shouldNotPropagateEmailSenderExceptionOnCreate() {
    // Arrange
    final EmailSenderException cause =
        EmailSenderException.becauseSmtpFailed("Connection refused");
    doThrow(cause).when(emailSenderPort).send(any());

    // Act & Assert
    assertDoesNotThrow(() -> service.notifyUserCreated(user));
    verify(emailSenderPort).send(any());
  }

  @Test
  @DisplayName("notifyUserUpdated() no propaga el fallo del puerto SMTP")
  void shouldNotPropagateEmailSenderExceptionOnUpdate() {
    // Arrange
    final EmailSenderException cause =
        EmailSenderException.becauseSmtpFailed("Connection refused");
    doThrow(cause).when(emailSenderPort).send(any());

    // Act & Assert
    assertDoesNotThrow(() -> service.notifyUserUpdated(user));
    verify(emailSenderPort).send(any());
  }

  private EmailNotificationRequest captureRequest() {
    final ArgumentCaptor<EmailNotificationRequest> captor =
        ArgumentCaptor.forClass(EmailNotificationRequest.class);
    verify(emailSenderPort).send(captor.capture());
    return captor.getValue();
  }
}
