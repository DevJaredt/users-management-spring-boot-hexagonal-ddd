package com.jcaa.usersmanagement.application.port.out.dto;

import java.util.Map;
import java.util.Objects;

/**
 * Solicitud de notificacion que la capa de aplicacion entrega al puerto de salida de correo.
 *
 * <p>Transporta la intencion de negocio (destinatario, asunto, plantilla y variables) pero no
 * contenido HTML: el render es responsabilidad de la infraestructura.
 */
public record EmailNotificationRequest(
    String recipientEmail,
    String recipientName,
    String subject,
    EmailTemplate template,
    Map<String, String> variables) {

  private static final String ERROR_RECIPIENT_EMAIL = "El email del destinatario es requerido.";
  private static final String ERROR_RECIPIENT_NAME = "El nombre del destinatario es requerido.";
  private static final String ERROR_SUBJECT = "El asunto es requerido.";
  private static final String ERROR_TEMPLATE = "La plantilla de notificacion es requerida.";
  private static final String ERROR_VARIABLES = "Las variables de la plantilla son requeridas.";

  public EmailNotificationRequest {
    recipientEmail = requireNotBlank(recipientEmail, ERROR_RECIPIENT_EMAIL);
    recipientName = requireNotBlank(recipientName, ERROR_RECIPIENT_NAME);
    subject = requireNotBlank(subject, ERROR_SUBJECT);
    template = Objects.requireNonNull(template, ERROR_TEMPLATE);
    variables = Map.copyOf(Objects.requireNonNull(variables, ERROR_VARIABLES));
  }

  private static String requireNotBlank(final String value, final String errorMessage) {
    Objects.requireNonNull(value, errorMessage);
    if (value.isBlank()) {
      throw new IllegalArgumentException(errorMessage);
    }
    return value;
  }
}
