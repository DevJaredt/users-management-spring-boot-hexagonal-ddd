package com.jcaa.usersmanagement.infrastructure.adapter.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.jcaa.usersmanagement.application.port.out.dto.EmailNotificationRequest;
import com.jcaa.usersmanagement.application.port.out.dto.EmailTemplate;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.infrastructure.adapter.email.template.ClasspathEmailTemplateRenderer;
import java.lang.reflect.Field;
import java.util.Map;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Tests for JavaMailEmailSenderAdapter.
 *
 * <p>Covers: successful dispatch rendering the real classpath template, EmailSenderException
 * wrapping when Transport.send() raises MessagingException, and the authenticator credentials
 * branch. Transport.send() is static — mocked with MockedStatic to prevent a real SMTP connection.
 */
@DisplayName("JavaMailEmailSenderAdapter")
class JavaMailEmailSenderAdapterTest {

  private static final String HOST = "smtp.example.com";
  private static final int PORT = 587;
  private static final String USERNAME = "user@example.com";
  private static final String PASSWORD = "secret";
  private static final String FROM_ADDRESS = "noreply@example.com";
  private static final String FROM_NAME = "App Notifications";
  private static final int TIMEOUT_MILLIS = 5000;
  private static final String DEST_EMAIL = "john@example.com";
  private static final String DEST_NAME = "John Doe";
  private static final String SUBJECT = "Account created";
  private static final String TOKEN_NAME = "name";
  private static final String TOKEN_EMAIL = "email";
  private static final String TOKEN_ROLE = "role";

  private JavaMailEmailSenderAdapter adapter;
  private EmailNotificationRequest request;

  @BeforeEach
  void setUp() {
    final SmtpConfig config =
        new SmtpConfig(
            HOST, PORT, USERNAME, PASSWORD, FROM_ADDRESS, FROM_NAME, TIMEOUT_MILLIS);
    adapter = new JavaMailEmailSenderAdapter(config, new ClasspathEmailTemplateRenderer());
    request =
        new EmailNotificationRequest(
            DEST_EMAIL,
            DEST_NAME,
            SUBJECT,
            EmailTemplate.USER_CREATED,
            Map.of(TOKEN_NAME, DEST_NAME, TOKEN_EMAIL, DEST_EMAIL, TOKEN_ROLE, "ADMIN"));
  }

  // ── send() — happy path

  @Test
  @DisplayName("send() renderiza la plantilla y llama a Transport.send() una sola vez")
  void shouldRenderTemplateAndDispatchMessageWhenSmtpSucceeds() {
    // Arrange
    try (final MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
      final ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

      // Act
      adapter.send(request);

      // Assert
      mockedTransport.verify(() -> Transport.send(messageCaptor.capture()));
      final Message sentMessage = messageCaptor.getValue();
      assertAll(
          "mensaje despachado",
          () -> assertEquals(SUBJECT, sentMessage.getSubject()),
          () ->
              assertTrue(
                  sentMessage.getContent().toString().contains(DEST_NAME),
                  "el cuerpo debe contener el nombre interpolado"),
          () ->
              assertFalse(
                  sentMessage.getContent().toString().contains("{{"),
                  "no deben quedar tokens sin sustituir"));
    }
  }

  // ── send() — MessagingException → EmailSenderException

  @Test
  @DisplayName("send() envuelve MessagingException en EmailSenderException sin exponer el email")
  void shouldWrapMessagingExceptionWithoutRecipientEmail() {
    // Declare the cause before the static mock to avoid intercepting
    // any static calls MessagingException's constructor may trigger
    final MessagingException smtpError = new MessagingException("Connection refused");

    // Arrange
    try (final MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
      mockedTransport.when(() -> Transport.send(any(Message.class))).thenThrow(smtpError);

      // Act & Assert
      final EmailSenderException exception =
          assertThrows(EmailSenderException.class, () -> adapter.send(request));
      assertAll(
          "excepcion de envio",
          () -> assertTrue(exception.getMessage().contains("Connection refused")),
          () ->
              assertFalse(
                  exception.getMessage().contains(DEST_EMAIL),
                  "el mensaje no debe contener el email del destinatario"));
    }
  }

  // ── Authenticator — getPasswordAuthentication() credentials branch

  @Test
  @SuppressWarnings("java:S3011") // reflection required to access private mailSession in test scope
  @DisplayName("Authenticator returns PasswordAuthentication with credentials from SmtpConfig")
  void shouldProvideConfiguredCredentialsWhenAuthenticatorIsInvoked() throws Exception {
    // Arrange — retrieve the private mailSession field to reach the stored Authenticator
    final Field sessionField = JavaMailEmailSenderAdapter.class.getDeclaredField("mailSession");
    sessionField.setAccessible(true);
    final Session mailSession = (Session) sessionField.get(adapter);

    // Act — Session.requestPasswordAuthentication() internally invokes the stored
    // Authenticator's getPasswordAuthentication(), covering that branch
    final PasswordAuthentication auth =
        mailSession.requestPasswordAuthentication(null, PORT, "smtp", "Login", USERNAME);

    // Assert
    assertAll(
        "credentials must match SmtpConfig",
        () -> assertEquals(USERNAME, auth.getUserName()),
        () -> assertEquals(PASSWORD, auth.getPassword()));
  }
}
