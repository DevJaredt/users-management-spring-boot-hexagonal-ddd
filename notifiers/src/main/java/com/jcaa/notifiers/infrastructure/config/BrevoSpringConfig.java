package com.jcaa.notifiers.infrastructure.config;

import com.jcaa.notifiers.infrastructure.adapter.email.brevo.BrevoConfig;
import com.jcaa.notifiers.infrastructure.adapter.email.brevo.BrevoRestClientFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cablea el cliente HTTP de Brevo con timeouts explicitos y la cabecera de autenticacion.
 *
 * <p>La API key nunca se registra completa en los logs: solo su huella SHA-256.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class BrevoSpringConfig {

  private static final String LOG_CONFIG_LOADED =
      "[BrevoSpringConfig] configuracion cargada. apiKeyFingerprint={} fromAddress={}";
  private static final String SHA_256 = "SHA-256";
  private static final int FINGERPRINT_BYTES = 6;

  @Bean
  public BrevoConfig brevoConfig(
      @Value("${brevo.base-url}") final String baseUrl,
      @Value("${brevo.api-key}") final String apiKey,
      @Value("${brevo.from.address}") final String fromAddress,
      @Value("${brevo.from.name}") final String fromName,
      @Value("${brevo.timeout.connect}") final Duration connectTimeout,
      @Value("${brevo.timeout.read}") final Duration readTimeout,
      @Value("${brevo.retry.max-attempts}") final int retryMaxAttempts,
      @Value("${brevo.retry.initial-delay}") final Duration retryInitialDelay,
      @Value("${brevo.retry.multiplier}") final double retryMultiplier,
      @Value("${brevo.circuit-breaker.failure-rate-threshold}") final int circuitFailureThreshold,
      @Value("${brevo.circuit-breaker.sliding-window-size}") final int circuitSlidingWindowSize,
      @Value("${brevo.circuit-breaker.minimum-calls}") final int circuitMinimumCalls,
      @Value("${brevo.circuit-breaker.open-duration}") final Duration circuitOpenDuration) {

    final BrevoConfig config =
        new BrevoConfig(
            baseUrl,
            apiKey,
            fromAddress,
            fromName,
            connectTimeout,
            readTimeout,
            retryMaxAttempts,
            retryInitialDelay,
            retryMultiplier,
            circuitFailureThreshold,
            circuitSlidingWindowSize,
            circuitMinimumCalls,
            circuitOpenDuration);

    log.info(LOG_CONFIG_LOADED, fingerprint(apiKey), fromAddress);
    return config;
  }

  @Bean
  public RestClient brevoRestClient(final BrevoConfig config) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(config.connectTimeout());
    requestFactory.setReadTimeout(config.readTimeout());
    return BrevoRestClientFactory.builder(config).requestFactory(requestFactory).build();
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
