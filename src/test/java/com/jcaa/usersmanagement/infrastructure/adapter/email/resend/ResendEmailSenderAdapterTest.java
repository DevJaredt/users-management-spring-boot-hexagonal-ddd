package com.jcaa.usersmanagement.infrastructure.adapter.email.resend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jcaa.usersmanagement.application.port.out.dto.EmailNotificationRequest;
import com.jcaa.usersmanagement.application.port.out.dto.EmailTemplate;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.infrastructure.adapter.email.template.ClasspathEmailTemplateRenderer;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests para ResendEmailSenderAdapter.
 *
 * <p>Cubre: publicacion del correo en la API de Resend con la cabecera de autorizacion y el HTML
 * renderizado, y el envoltorio de errores HTTP en EmailSenderException sin filtrar el email del
 * destinatario.
 */
@DisplayName("ResendEmailSenderAdapter")
class ResendEmailSenderAdapterTest {

  private static final String API_KEY = "re_test_api_key";
  private static final String FROM_ADDRESS = "noreply@example.com";
  private static final String FROM_NAME = "App Notifications";
  private static final String BASE_URL = "https://api.resend.com";
  private static final int TIMEOUT_MILLIS = 5000;
  private static final String DEST_EMAIL = "john@example.com";
  private static final String DEST_NAME = "John Doe";
  private static final String SUBJECT = "Tu cuenta ha sido creada";

  private MockRestServiceServer mockServer;
  private ResendEmailSenderAdapter adapter;
  private EmailNotificationRequest request;

  @BeforeEach
  void setUp() {
    final ResendConfig config =
        new ResendConfig(API_KEY, FROM_ADDRESS, FROM_NAME, BASE_URL, TIMEOUT_MILLIS);
    final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    mockServer = MockRestServiceServer.bindTo(builder).build();
    adapter =
        new ResendEmailSenderAdapter(
            builder.build(), config, new ClasspathEmailTemplateRenderer());
    request =
        new EmailNotificationRequest(
            DEST_EMAIL,
            DEST_NAME,
            SUBJECT,
            EmailTemplate.USER_CREATED,
            Map.of("name", DEST_NAME, "email", DEST_EMAIL, "role", "ADMIN"));
  }

  // ── send() — happy path

  @Test
  @DisplayName("send() publica el correo en /emails con la API key y el HTML renderizado")
  void shouldPostEmailWithApiKeyAndRenderedHtml() {
    // Arrange
    mockServer
        .expect(requestTo(BASE_URL + "/emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer " + API_KEY))
        .andExpect(jsonPath("$.from").value(FROM_NAME + " <" + FROM_ADDRESS + ">"))
        .andExpect(jsonPath("$.to[0]").value(DEST_EMAIL))
        .andExpect(jsonPath("$.subject").value(SUBJECT))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(DEST_NAME)))
        .andRespond(withSuccess("{\"id\":\"email_123\"}", MediaType.APPLICATION_JSON));

    // Act
    adapter.send(request);

    // Assert
    mockServer.verify();
  }

  // ── send() — error HTTP → EmailSenderException sin PII

  @Test
  @DisplayName("send() envuelve el error HTTP sin incluir el email del destinatario")
  void shouldWrapHttpErrorWithoutRecipientEmail() {
    // Arrange
    mockServer
        .expect(requestTo(BASE_URL + "/emails"))
        .andRespond(
            withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .body("{\"message\":\"Invalid to field: " + DEST_EMAIL + "\"}")
                .contentType(MediaType.APPLICATION_JSON));

    // Act
    final EmailSenderException exception =
        assertThrows(EmailSenderException.class, () -> adapter.send(request));

    // Assert
    assertAll(
        "excepcion del proveedor",
        () -> assertTrue(exception.getMessage().contains("422"), "debe incluir el codigo HTTP"),
        () ->
            assertFalse(
                exception.getMessage().contains(DEST_EMAIL),
                "no debe filtrar el email del destinatario"));
  }
}
