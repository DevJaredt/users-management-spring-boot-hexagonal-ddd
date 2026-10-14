package com.jcaa.notifiers.infrastructure.adapter.email.brevo;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jcaa.notifiers.application.exception.EmailDeliveryException;
import com.jcaa.notifiers.application.port.out.dto.EmailMessage;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests del adaptador de Brevo.
 *
 * <p>Cubre: publicacion del correo en {@code /v3/smtp/email} con la cabecera de autenticacion y la
 * clave de idempotencia, y el envoltorio de errores en {@link EmailDeliveryException} sin filtrar
 * el email del destinatario.
 */
@DisplayName("BrevoEmailSenderAdapter")
class BrevoEmailSenderAdapterTest {

  private static final String API_KEY = "xkeysib-test-key";
  private static final String FROM_ADDRESS = "noreply@example.com";
  private static final String FROM_NAME = "App Notifications";
  private static final String BASE_URL = "https://api.brevo.com";
  private static final String DEST_EMAIL = "john@example.com";
  private static final String DEST_NAME = "John Doe";
  private static final String SUBJECT = "Tu cuenta ha sido creada";
  private static final String API_KEY_HEADER = "api-key";
  private static final String IDEMPOTENCY_HEADER = "idempotencyKey";

  private MockRestServiceServer mockServer;
  private BrevoEmailSenderAdapter adapter;

  @BeforeEach
  void setUp() {
    final BrevoConfig config =
        new BrevoConfig(
            BASE_URL,
            API_KEY,
            FROM_ADDRESS,
            FROM_NAME,
            Duration.ofSeconds(2),
            Duration.ofSeconds(5),
            3,
            Duration.ofMillis(1),
            2.0,
            50,
            10,
            5,
            Duration.ofSeconds(30));
    final RestClient.Builder builder = BrevoRestClientFactory.builder(config);
    mockServer = MockRestServiceServer.bindTo(builder).build();
    adapter = new BrevoEmailSenderAdapter(config, builder.build());
  }

  @Test
  @DisplayName("send() publica en /v3/smtp/email con api-key, idempotencia y el HTML")
  void shouldPostEmailWithApiKeyAndIdempotencyKey() {
    // Arrange
    mockServer
        .expect(requestTo(BASE_URL + "/v3/smtp/email"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(API_KEY_HEADER, API_KEY))
        .andExpect(jsonPath("$.sender.email").value(FROM_ADDRESS))
        .andExpect(jsonPath("$.to[0].email").value(DEST_EMAIL))
        .andExpect(jsonPath("$.subject").value(SUBJECT))
        .andExpect(jsonPath("$.headers." + IDEMPOTENCY_HEADER, notNullValue()))
        .andRespond(withSuccess("{\"messageId\":\"1\"}", MediaType.APPLICATION_JSON));

    // Act
    adapter.send(message());

    // Assert
    mockServer.verify();
  }

  @Test
  @DisplayName("send() envuelve un 400 en EmailDeliveryException sin incluir el destinatario")
  void shouldWrapClientErrorWithoutRecipientEmail() {
    // Arrange — 400 no es transitorio, asi que no reintenta y es rapido de testear
    mockServer
        .expect(requestTo(BASE_URL + "/v3/smtp/email"))
        .andRespond(
            withStatus(HttpStatus.BAD_REQUEST)
                .body("{\"message\":\"invalid to: " + DEST_EMAIL + "\"}")
                .contentType(MediaType.APPLICATION_JSON));

    // Act
    final EmailDeliveryException exception =
        assertThrows(EmailDeliveryException.class, () -> adapter.send(message()));

    // Assert
    assertAll(
        "excepcion del proveedor",
        () -> assertTrue(exception.getMessage().contains("400"), "debe incluir el codigo HTTP"),
        () ->
            assertFalse(
                exception.getMessage().contains(DEST_EMAIL),
                "no debe filtrar el email del destinatario"));
  }

  private static EmailMessage message() {
    return new EmailMessage(DEST_EMAIL, DEST_NAME, SUBJECT, "<p>Hola</p>");
  }
}
