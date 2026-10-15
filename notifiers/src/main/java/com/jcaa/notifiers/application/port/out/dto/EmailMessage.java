package com.jcaa.notifiers.application.port.out.dto;

import java.util.Objects;

/**
 * Mensaje de correo listo para enviar, en terminos de negocio.
 *
 * <p>El puerto de salida lo recibe y la infraestructura decide como se entrega (relay SMTP de
 * Brevo).
 */
public record EmailMessage(
    String recipientEmail, String recipientName, String subject, String htmlBody) {

  private static final String ERROR_RECIPIENT_EMAIL = "El email del destinatario es requerido.";
  private static final String ERROR_RECIPIENT_NAME = "El nombre del destinatario es requerido.";
  private static final String ERROR_SUBJECT = "El asunto es requerido.";
  private static final String ERROR_BODY = "El cuerpo del mensaje es requerido.";

  public EmailMessage {
    recipientEmail = requireNotBlank(recipientEmail, ERROR_RECIPIENT_EMAIL);
    recipientName = requireNotBlank(recipientName, ERROR_RECIPIENT_NAME);
    subject = requireNotBlank(subject, ERROR_SUBJECT);
    htmlBody = requireNotBlank(htmlBody, ERROR_BODY);
  }

  private static String requireNotBlank(final String value, final String errorMessage) {
    Objects.requireNonNull(value, errorMessage);
    if (value.isBlank()) {
      throw new IllegalArgumentException(errorMessage);
    }
    return value;
  }
}
