package com.jcaa.notifiers.infrastructure.adapter.email.brevo;

import java.time.Duration;

/**
 * Configuracion del relay SMTP de Brevo ({@code smtp-relay.brevo.com}).
 *
 * <p>Se valida en la construccion para que una configuracion invalida falle al arrancar en lugar de
 * en el primer envio.
 */
public record BrevoConfig(
    String host,
    int port,
    boolean auth,
    boolean startTlsEnabled,
    boolean sslEnabled,
    String username,
    String password,
    String fromAddress,
    String fromName,
    Duration connectTimeout,
    Duration readTimeout,
    Duration writeTimeout,
    int retryMaxAttempts,
    Duration retryInitialDelay,
    double retryMultiplier,
    int circuitFailureThreshold,
    int circuitSlidingWindowSize,
    int circuitMinimumCalls,
    Duration circuitOpenDuration) {

  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 65_535;

  public BrevoConfig {
    requireText(host, "brevo.smtp.host / BREVO_SMTP_HOST");
    requireText(username, "brevo.smtp.username / BREVO_SMTP_USERNAME");
    requireText(password, "brevo.smtp.password / BREVO_SMTP_PASSWORD");
    requireText(fromAddress, "brevo.from.address / BREVO_FROM_ADDRESS");
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException(
          "brevo.smtp.port / BREVO_SMTP_PORT debe estar entre "
              + MIN_PORT
              + " y "
              + MAX_PORT
              + ".");
    }
  }

  private static void requireText(final String value, final String propertyName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
          "La configuracion requerida '" + propertyName + "' no puede estar vacia.");
    }
  }
}
