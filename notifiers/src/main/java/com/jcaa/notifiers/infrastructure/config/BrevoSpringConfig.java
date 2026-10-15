package com.jcaa.notifiers.infrastructure.config;

import com.jcaa.notifiers.infrastructure.adapter.email.brevo.BrevoConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Cablea el relay SMTP de Brevo con timeouts explicitos, autenticacion y TLS.
 *
 * <p>La contrasena SMTP nunca se registra completa en los logs: solo su huella SHA-256. El login
 * SMTP de Brevo ({@code beXXXXXXX@smtp-brevo.com}) tampoco se registra.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class BrevoSpringConfig {

  private static final String LOG_CONFIG_LOADED =
      "[BrevoSpringConfig] configuracion cargada. host={}:{} passwordFingerprint={} fromAddress={}";
  private static final String SHA_256 = "SHA-256";
  private static final int FINGERPRINT_BYTES = 6;

  @Bean
  public BrevoConfig brevoConfig(
      @Value("${brevo.smtp.host}") final String host,
      @Value("${brevo.smtp.port}") final int port,
      @Value("${brevo.smtp.auth}") final boolean auth,
      @Value("${brevo.smtp.starttls.enabled}") final boolean startTlsEnabled,
      @Value("${brevo.smtp.ssl.enabled}") final boolean sslEnabled,
      @Value("${brevo.smtp.username}") final String username,
      @Value("${brevo.smtp.password}") final String password,
      @Value("${brevo.from.address}") final String fromAddress,
      @Value("${brevo.from.name}") final String fromName,
      @Value("${brevo.timeout.connect}") final Duration connectTimeout,
      @Value("${brevo.timeout.read}") final Duration readTimeout,
      @Value("${brevo.timeout.write}") final Duration writeTimeout,
      @Value("${brevo.retry.max-attempts}") final int retryMaxAttempts,
      @Value("${brevo.retry.initial-delay}") final Duration retryInitialDelay,
      @Value("${brevo.retry.multiplier}") final double retryMultiplier,
      @Value("${brevo.circuit-breaker.failure-rate-threshold}") final int circuitFailureThreshold,
      @Value("${brevo.circuit-breaker.sliding-window-size}") final int circuitSlidingWindowSize,
      @Value("${brevo.circuit-breaker.minimum-calls}") final int circuitMinimumCalls,
      @Value("${brevo.circuit-breaker.open-duration}") final Duration circuitOpenDuration) {

    final BrevoConfig config =
        new BrevoConfig(
            host,
            port,
            auth,
            startTlsEnabled,
            sslEnabled,
            username,
            password,
            fromAddress,
            fromName,
            connectTimeout,
            readTimeout,
            writeTimeout,
            retryMaxAttempts,
            retryInitialDelay,
            retryMultiplier,
            circuitFailureThreshold,
            circuitSlidingWindowSize,
            circuitMinimumCalls,
            circuitOpenDuration);

    log.info(LOG_CONFIG_LOADED, host, port, fingerprint(password), fromAddress);
    return config;
  }

  @Bean
  public JavaMailSender brevoMailSender(final BrevoConfig config) {
    final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(config.host());
    mailSender.setPort(config.port());
    mailSender.setUsername(config.username());
    mailSender.setPassword(config.password());
    mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

    final Properties properties = mailSender.getJavaMailProperties();
    properties.put("mail.transport.protocol", "smtp");
    properties.put("mail.smtp.auth", Boolean.toString(config.auth()));
    // STARTTLS oportunista: se usa si el servidor lo ofrece (puertos 587/2525) sin exigirlo, para
    // no romper el envio cuando Render bloquea el 587 y se usa el 2525.
    properties.put("mail.smtp.starttls.enable", Boolean.toString(config.startTlsEnabled()));
    properties.put("mail.smtp.starttls.required", "false");
    properties.put("mail.smtp.ssl.enable", Boolean.toString(config.sslEnabled()));
    properties.put(
        "mail.smtp.connectiontimeout", Long.toString(config.connectTimeout().toMillis()));
    properties.put("mail.smtp.timeout", Long.toString(config.readTimeout().toMillis()));
    properties.put("mail.smtp.writetimeout", Long.toString(config.writeTimeout().toMillis()));
    return mailSender;
  }

  private static String fingerprint(final String value) {
    try {
      final byte[] digest =
          MessageDigest.getInstance(SHA_256).digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest, 0, FINGERPRINT_BYTES);
    } catch (final NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 no esta disponible", exception);
    }
  }
}
