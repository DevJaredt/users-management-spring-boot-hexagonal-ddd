package com.jcaa.notifiers.application.exception;

/**
 * Fallo al entregar una notificacion por el proveedor de correo.
 *
 * <p>El mensaje nunca incluye el email del destinatario: es dato personal y no debe llegar a los
 * logs (ver AGENTS.md, "No PII in logs").
 */
public class EmailDeliveryException extends RuntimeException {

  private static final String MESSAGE_WITH_DETAIL =
      "No se pudo enviar la notificacion por correo. Error del proveedor: %s";

  public EmailDeliveryException(final String message) {
    super(message);
  }

  public EmailDeliveryException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static EmailDeliveryException becauseProviderFailed(final String providerError) {
    return new EmailDeliveryException(String.format(MESSAGE_WITH_DETAIL, providerError));
  }
}
