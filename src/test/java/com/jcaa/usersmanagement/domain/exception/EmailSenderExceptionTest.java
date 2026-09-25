package com.jcaa.usersmanagement.domain.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests para EmailSenderException.
 *
 * <p>De paso cubre el constructor de dos argumentos de {@code DomainException} (abstracta),
 * alcanzable sólo a través de {@code becauseSendFailed(cause)}.
 */
@DisplayName("EmailSenderException")
class EmailSenderExceptionTest {

  // ── becauseSmtpFailed()

  @Test
  @DisplayName("becauseSmtpFailed() debe formatear el mensaje con el error SMTP y sin datos personales")
  void shouldFormatMessageWithSmtpErrorAndNoPersonalData() {
    // Arrange
    final String destinationEmail = "user@example.com";
    final String smtpError = "Connection refused";

    // Act
    final String message = EmailSenderException.becauseSmtpFailed(smtpError).getMessage();

    // Assert
    assertAll(
        "becauseSmtpFailed",
        () -> assertTrue(message.contains(smtpError), "el mensaje debe contener el error SMTP"),
        () ->
            assertFalse(
                message.contains(destinationEmail),
                "el mensaje no debe contener el email del destinatario (PII en logs)"));
  }

  // ── becauseProviderFailed()

  @Test
  @DisplayName("becauseProviderFailed() debe formatear el mensaje con el detalle del proveedor")
  void shouldFormatMessageWithProviderError() {
    // Arrange
    final String providerError = "HTTP 422 Unprocessable Entity";

    // Act
    final String message = EmailSenderException.becauseProviderFailed(providerError).getMessage();

    // Assert
    assertAll(
        "becauseProviderFailed",
        () -> assertTrue(message.contains(providerError), "el mensaje debe contener el detalle"),
        () -> assertTrue(message.contains("proveedor"), "debe identificar el origen del fallo"));
  }

  // ── becauseSendFailed()

  @Test
  @DisplayName("becauseSendFailed() debe encapsular la causa y producir un mensaje no vacío")
  void shouldWrapCauseAndProduceNonBlankMessage() {
    // Arrange
    final Throwable cause = new RuntimeException("IO error");

    // Act
    final EmailSenderException exception = EmailSenderException.becauseSendFailed(cause);

    // Assert
    assertAll(
        "becauseSendFailed",
        () -> assertSame(cause, exception.getCause(), "debe encapsular la causa original"),
        () ->
            assertFalse(
                exception.getMessage().isBlank(), "el mensaje por defecto no debe estar vacío"));
  }
}
