package com.jcaa.usersmanagement.application.port.out.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmailNotificationRequest")
class EmailNotificationRequestTest {

  private static final String EMAIL = "john@example.com";
  private static final String NAME = "John";
  private static final String SUBJECT = "Cuenta creada";
  private static final Map<String, String> VARIABLES = Map.of("name", NAME);

  @Test
  @DisplayName("construye una solicitud valida con copia defensiva de las variables")
  void shouldBuildValidRequestWithImmutableVariables() {
    // Arrange
    final Map<String, String> mutableVariables = new HashMap<>(VARIABLES);

    // Act
    final EmailNotificationRequest request =
        new EmailNotificationRequest(
            EMAIL, NAME, SUBJECT, EmailTemplate.USER_CREATED, mutableVariables);
    mutableVariables.put("extra", "value");

    // Assert
    assertAll(
        "solicitud valida",
        () -> assertEquals(EMAIL, request.recipientEmail()),
        () -> assertEquals(NAME, request.recipientName()),
        () -> assertEquals(SUBJECT, request.subject()),
        () -> assertEquals(EmailTemplate.USER_CREATED, request.template()),
        () ->
            assertFalse(
                request.variables().containsKey("extra"),
                "las variables deben copiarse defensivamente"),
        () ->
            assertThrows(
                UnsupportedOperationException.class, () -> request.variables().put("x", "y")));
  }

  @Test
  @DisplayName("rechaza destinatario, asunto, plantilla o variables invalidos")
  void shouldRejectInvalidValues() {
    assertAll(
        "validacion de la solicitud",
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new EmailNotificationRequest(
                        null, NAME, SUBJECT, EmailTemplate.USER_CREATED, VARIABLES)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () ->
                    new EmailNotificationRequest(
                        "   ", NAME, SUBJECT, EmailTemplate.USER_CREATED, VARIABLES)),
        () ->
            assertThrows(
                NullPointerException.class,
                () ->
                    new EmailNotificationRequest(
                        EMAIL, null, SUBJECT, EmailTemplate.USER_CREATED, VARIABLES)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new EmailNotificationRequest(EMAIL, NAME, "", EmailTemplate.USER_CREATED, VARIABLES)),
        () ->
            assertThrows(
                NullPointerException.class,
                () -> new EmailNotificationRequest(EMAIL, NAME, SUBJECT, null, VARIABLES)),
        () ->
            assertThrows(
                NullPointerException.class,
                () -> new EmailNotificationRequest(EMAIL, NAME, SUBJECT, EmailTemplate.USER_CREATED, null)));
  }
}
