package com.jcaa.usersmanagement.domain.exception;

public final class EmailSenderException extends DomainException {

  private static final String DEFAULT_MESSAGE = "La notificación por correo no pudo ser enviada.";
  private static final String MESSAGE_WITH_DETAIL =
      "No se pudo enviar la notificación por correo. Error SMTP: %s";
  private static final String MESSAGE_PROVIDER_DETAIL =
      "No se pudo enviar la notificación por correo. Error del proveedor: %s";

  public EmailSenderException(final String message) {
    super(message);
  }

  public EmailSenderException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * El mensaje NO incluye el email del destinatario: es dato personal y no debe acabar en los
   * logs (ver AGENTS.md, "No PII in logs").
   */
  public static EmailSenderException becauseSmtpFailed(final String smtpError) {
    return new EmailSenderException(String.format(MESSAGE_WITH_DETAIL, smtpError));
  }

  /**
   * Para proveedores de correo por API (por ejemplo Resend). El detalle tampoco debe contener el
   * email del destinatario.
   */
  public static EmailSenderException becauseProviderFailed(final String providerError) {
    return new EmailSenderException(String.format(MESSAGE_PROVIDER_DETAIL, providerError));
  }

  public static EmailSenderException becauseSendFailed(final Throwable cause) {
    return new EmailSenderException(DEFAULT_MESSAGE, cause);
  }
}
