package com.jcaa.usersmanagement.infrastructure.adapter.email.resend;

import java.util.Objects;

/**
 * Configuracion del proveedor Resend (API HTTPS).
 *
 * <p>Se valida en la construccion para que una configuracion invalida falle al arrancar en lugar
 * de en el primer envio.
 */
public record ResendConfig(
    String apiKey, String fromAddress, String fromName, String baseUrl, int timeoutMillis) {

  private static final String ERROR_API_KEY = "La API key de Resend es requerida.";
  private static final String ERROR_FROM_ADDRESS = "La direccion remitente es requerida.";
  private static final String ERROR_FROM_NAME = "El nombre remitente es requerido.";
  private static final String ERROR_BASE_URL = "La URL base de Resend es requerida.";
  private static final String ERROR_TIMEOUT = "El timeout de Resend debe ser mayor que cero.";

  public ResendConfig {
    requireNotBlank(apiKey, ERROR_API_KEY);
    requireNotBlank(fromAddress, ERROR_FROM_ADDRESS);
    requireNotBlank(fromName, ERROR_FROM_NAME);
    requireNotBlank(baseUrl, ERROR_BASE_URL);
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
