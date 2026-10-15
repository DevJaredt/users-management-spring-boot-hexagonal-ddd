package com.jcaa.notifiers.infrastructure.adapter.email.brevo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.notifiers.application.exception.EmailDeliveryException;
import com.jcaa.notifiers.application.port.out.dto.EmailMessage;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Tests del adaptador SMTP de Brevo.
 *
 * <p>Cubre: la composicion del {@link MimeMessage} HTML con remitente y destinatario, el reintento
 * ante fallos transitorios y el envoltorio de errores en {@link EmailDeliveryException} sin filtrar
 * el email del destinatario.
 */
@DisplayName("BrevoEmailSenderAdapter")
@ExtendWith(MockitoExtension.class)
class BrevoEmailSenderAdapterTest {

  private static final String HOST = "smtp-relay.brevo.com";
  private static final int PORT = 587;
  private static final String USERNAME = "smtp-user@example.com";
  private static final String PASSWORD = "xsmtpsib-test-key";
  private static final String FROM_ADDRESS = "noreply@example.com";
  private static final String FROM_NAME = "App Notifications";
  private static final String DEST_EMAIL = "john@example.com";
  private static final String DEST_NAME = "John Doe";
  private static final String SUBJECT = "Tu cuenta ha sido creada";
  private static final String HTML_BODY = "<p>Hola</p>";

  @Mock private JavaMailSender mailSender;

  private MimeMessage mimeMessage;
  private BrevoEmailSenderAdapter adapter;

  @BeforeEach
  void setUp() {
    final BrevoConfig config =
        new BrevoConfig(
            HOST,
            PORT,
            true,
            true,
            false,
            USERNAME,
            PASSWORD,
            FROM_ADDRESS,
            FROM_NAME,
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            Duration.ofSeconds(10),
            3,
            Duration.ofMillis(1),
            2.0,
            50,
            10,
            5,
            Duration.ofSeconds(30));
    mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    adapter = new BrevoEmailSenderAdapter(config, mailSender);
  }

  @Test
  @DisplayName("send() entrega por SMTP un MimeMessage HTML con remitente y destinatario")
  void shouldSendHtmlMessage() throws Exception {
    // Act
    adapter.send(message());

    // Assert
    verify(mailSender).send(mimeMessage);
    final InternetAddress sender = (InternetAddress) mimeMessage.getFrom()[0];
    final InternetAddress recipient = (InternetAddress) mimeMessage.getAllRecipients()[0];
    assertAll(
        "mensaje SMTP",
        () -> assertEquals(FROM_ADDRESS, sender.getAddress()),
        () -> assertEquals(FROM_NAME, sender.getPersonal()),
        () -> assertEquals(DEST_EMAIL, recipient.getAddress()),
        () -> assertEquals(DEST_NAME, recipient.getPersonal()),
        () -> assertEquals(SUBJECT, mimeMessage.getSubject()),
        () ->
            assertTrue(
                mimeMessage.getDataHandler().getContentType().toLowerCase().contains("text/html")),
        () -> assertEquals(HTML_BODY, mimeMessage.getContent()));
  }

  @Test
  @DisplayName("send() reintenta ante un fallo transitorio y termina enviando")
  void shouldRetryOnTransientFailure() {
    // Arrange — el primer intento falla por conexion, el segundo se entrega
    doThrow(new MailSendException("connection reset"))
        .doNothing()
        .when(mailSender)
        .send(any(MimeMessage.class));

    // Act
    adapter.send(message());

    // Assert
    verify(mailSender, times(2)).send(any(MimeMessage.class));
  }

  @Test
  @DisplayName("send() envuelve un fallo de autenticacion sin incluir el destinatario")
  void shouldWrapAuthenticationFailureWithoutRecipientEmail() {
    // Arrange — la autenticacion no es transitoria, asi que no reintenta
    doThrow(new MailAuthenticationException("535 auth failed"))
        .when(mailSender)
        .send(any(MimeMessage.class));

    // Act
    final EmailDeliveryException exception =
        assertThrows(EmailDeliveryException.class, () -> adapter.send(message()));

    // Assert
    assertAll(
        "excepcion del proveedor",
        () ->
            assertTrue(
                exception.getMessage().contains("autenticacion"),
                "debe indicar el fallo de autenticacion"),
        () ->
            assertFalse(
                exception.getMessage().contains(DEST_EMAIL),
                "no debe filtrar el email del destinatario"));
  }

  private static EmailMessage message() {
    return new EmailMessage(DEST_EMAIL, DEST_NAME, SUBJECT, HTML_BODY);
  }
}
