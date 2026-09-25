package com.jcaa.usersmanagement.infrastructure.adapter.email;

import java.util.Objects;

/**
 * Configuracion SMTP del adaptador de correo.
 *
 * <p>Se valida en la construccion para que una configuracion invalida falle al arrancar la
 * aplicacion en lugar de en el primer envio.
 */
public record SmtpConfig(
    String host,
    int port,
    String username,
    String password,
    String fromAddress,
    String fromName,
    int timeoutMillis) {

  private static final String ERROR_HOST = "El host SMTP es requerido.";
  private static final String ERROR_PORT = "El puerto SMTP debe ser mayor que cero.";
  private static final String ERROR_FROM_ADDRESS = "La direccion remitente es requerida.";
  private static final String ERROR_TIMEOUT = "El timeout SMTP debe ser mayor que cero.";

  public SmtpConfig {
    requireNotBlank(host, ERROR_HOST);
    requirePositive(port, ERROR_PORT);
    requireNotBlank(fromAddress, ERROR_FROM_ADDRESS);
    requirePositive(timeoutMillis, ERROR_TIMEOUT);
  }

  private static void requireNotBlank(final String value, final String errorMessage) {
    Objects.requireNonNull(value, errorMessage);
    if (value.isBlank()) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  private static void requirePositive(final int value, final String errorMessage) {
    if (value <= 0) {
      throw new IllegalArgumentException(errorMessage);
    }
  }
}
