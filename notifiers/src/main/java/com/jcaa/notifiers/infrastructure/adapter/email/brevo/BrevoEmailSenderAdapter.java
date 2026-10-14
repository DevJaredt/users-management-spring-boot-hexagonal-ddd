package com.jcaa.notifiers.infrastructure.adapter.email.brevo;

import com.jcaa.notifiers.application.exception.EmailDeliveryException;
import com.jcaa.notifiers.application.port.out.EmailSenderPort;
import com.jcaa.notifiers.application.port.out.dto.EmailMessage;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptador del puerto de correo sobre la API REST de Brevo ({@code POST /v3/smtp/email}).
 *
 * <p>Resiliencia:
 *
 * <ul>
 *   <li><b>Retry</b> con backoff exponencial, solo para fallos transitorios (red, 408, 429, 5xx).
 *   <li><b>Circuit breaker</b> para no castigar al proveedor cuando esta caido.
 *   <li><b>Idempotencia</b>: cada intento lleva la misma cabecera {@code idempotencyKey}, asi un
 *       reintento no duplica el correo.
 * </ul>
 */
@Slf4j
@Component
public class BrevoEmailSenderAdapter implements EmailSenderPort {

  private static final String SEND_EMAIL_PATH = "/v3/smtp/email";
  private static final String IDEMPOTENCY_HEADER = "idempotencyKey";
  private static final String LOG_SENT = "[BrevoEmailSenderAdapter] correo aceptado por Brevo.";
  private static final String LOG_RETRY =
      "[BrevoEmailSenderAdapter] reintento {} tras fallo transitorio: {}";
  private static final String CIRCUIT_OPEN_DETAIL = "circuit breaker abierto";

  private final BrevoConfig config;
  private final RestClient restClient;
  private final Retry retry;
  private final CircuitBreaker circuitBreaker;

  public BrevoEmailSenderAdapter(final BrevoConfig config, final RestClient brevoRestClient) {
    this.config = config;
    this.restClient = brevoRestClient;
    this.retry = buildRetry(config);
    this.circuitBreaker = buildCircuitBreaker(config);
    this.retry
        .getEventPublisher()
        .onRetry(
            event ->
                log.warn(
                    LOG_RETRY,
                    event.getNumberOfRetryAttempts(),
                    exceptionType(event.getLastThrowable())));
  }

  @Override
  public void send(final EmailMessage message) {
    final BrevoEmailRequest request = buildRequest(message);
    final Runnable protectedRequest =
        Retry.decorateRunnable(
            retry, CircuitBreaker.decorateRunnable(circuitBreaker, () -> sendRequest(request)));
    try {
      protectedRequest.run();
      log.info(LOG_SENT);
    } catch (final RestClientResponseException exception) {
      // No se incluye el cuerpo de la respuesta del proveedor: puede repetir datos personales.
      throw EmailDeliveryException.becauseProviderFailed(
          "HTTP " + exception.getStatusCode().value());
    } catch (final CallNotPermittedException exception) {
      throw EmailDeliveryException.becauseProviderFailed(CIRCUIT_OPEN_DETAIL);
    } catch (final RestClientException exception) {
      throw EmailDeliveryException.becauseProviderFailed(exception.getClass().getSimpleName());
    }
  }

  private void sendRequest(final BrevoEmailRequest request) {
    restClient.post().uri(SEND_EMAIL_PATH).body(request).retrieve().toBodilessEntity();
  }

  private BrevoEmailRequest buildRequest(final EmailMessage message) {
    return new BrevoEmailRequest(
        new BrevoEmailRequest.Contact(config.fromAddress(), config.fromName()),
        List.of(
            new BrevoEmailRequest.Contact(message.recipientEmail(), message.recipientName())),
        message.subject(),
        message.htmlBody(),
        Map.of(IDEMPOTENCY_HEADER, UUID.randomUUID().toString()));
  }

  private static Retry buildRetry(final BrevoConfig config) {
    final IntervalFunction backoff =
        IntervalFunction.ofExponentialBackoff(
            config.retryInitialDelay().toMillis(), config.retryMultiplier());
    final RetryConfig retryConfig =
        RetryConfig.custom()
            .maxAttempts(config.retryMaxAttempts())
            .intervalFunction(backoff)
            .retryOnException(BrevoEmailSenderAdapter::isTransientFailure)
            .build();
    return Retry.of("brevo-email", retryConfig);
  }

  private static CircuitBreaker buildCircuitBreaker(final BrevoConfig config) {
    final Predicate<Throwable> transientFailure = BrevoEmailSenderAdapter::isTransientFailure;
    final CircuitBreakerConfig circuitConfig =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(config.circuitFailureThreshold())
            .slidingWindowSize(config.circuitSlidingWindowSize())
            .minimumNumberOfCalls(config.circuitMinimumCalls())
            .waitDurationInOpenState(config.circuitOpenDuration())
            .recordException(transientFailure)
            .build();
    return CircuitBreaker.of("brevo-email", circuitConfig);
  }

  private static boolean isTransientFailure(final Throwable exception) {
    if (exception instanceof ResourceAccessException
        || exception instanceof HttpServerErrorException) {
      return true;
    }
    if (exception instanceof HttpClientErrorException clientError) {
      final HttpStatusCode status = clientError.getStatusCode();
      return status.value() == 408 || status.value() == 429;
    }
    return false;
  }

  private static String exceptionType(final Throwable exception) {
    return Objects.isNull(exception) ? "causa desconocida" : exception.getClass().getSimpleName();
  }
}
