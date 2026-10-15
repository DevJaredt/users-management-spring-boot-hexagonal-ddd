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
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto de correo sobre el relay SMTP de Brevo.
 *
 * <p>Compone un {@link MimeMessage} con cuerpo HTML y lo entrega con {@link JavaMailSender}.
 *
 * <p>Resiliencia:
 *
 * <ul>
 *   <li><b>Retry</b> con backoff exponencial, solo para fallos transitorios (conexion, timeouts,
 *       4xx/5xx del servidor SMTP).
 *   <li><b>Circuit breaker</b> para no castigar al proveedor cuando esta caido.
 *   <li><b>Sin idempotencia</b>: SMTP no admite clave de idempotencia, asi que el mensaje se
 *       recompone en cada intento. Los reintentos se limitan a fallos de transporte para no
 *       duplicar el correo enviado.
 * </ul>
 */
@Slf4j
@Component
public class BrevoEmailSenderAdapter implements EmailSenderPort {

  private static final String LOG_SENT = "[BrevoEmailSenderAdapter] correo entregado al relay SMTP.";
  private static final String LOG_RETRY =
      "[BrevoEmailSenderAdapter] reintento {} tras fallo transitorio: {}";
  private static final String CIRCUIT_OPEN_DETAIL = "circuit breaker abierto";
  private static final String AUTH_FAILURE_DETAIL = "autenticacion SMTP rechazada";
  private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";
  private static final String COMPOSE_FAILURE = "No se pudo componer el correo.";

  private final BrevoConfig config;
  private final JavaMailSender mailSender;
  private final Retry retry;
  private final CircuitBreaker circuitBreaker;

  public BrevoEmailSenderAdapter(final BrevoConfig config, final JavaMailSender mailSender) {
    this.config = config;
    this.mailSender = mailSender;
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
    final Runnable protectedRequest =
        Retry.decorateRunnable(
            retry, CircuitBreaker.decorateRunnable(circuitBreaker, () -> sendRequest(message)));
    try {
      protectedRequest.run();
      log.info(LOG_SENT);
    } catch (final MailException exception) {
      // No se incluye el mensaje del proveedor: puede repetir datos personales del destinatario.
      throw EmailDeliveryException.becauseProviderFailed(providerDetail(exception));
    } catch (final CallNotPermittedException exception) {
      throw EmailDeliveryException.becauseProviderFailed(CIRCUIT_OPEN_DETAIL);
    }
  }

  private void sendRequest(final EmailMessage message) {
    mailSender.send(buildMimeMessage(message));
  }

  private MimeMessage buildMimeMessage(final EmailMessage message) {
    final MimeMessage mimeMessage = mailSender.createMimeMessage();
    try {
      mimeMessage.setFrom(address(config.fromAddress(), config.fromName()));
      mimeMessage.setRecipient(
          Message.RecipientType.TO, address(message.recipientEmail(), message.recipientName()));
      mimeMessage.setSubject(message.subject(), StandardCharsets.UTF_8.name());
      mimeMessage.setContent(message.htmlBody(), HTML_CONTENT_TYPE);
    } catch (final MessagingException | UnsupportedEncodingException exception) {
      throw new MailPreparationException(COMPOSE_FAILURE, exception);
    }
    return mimeMessage;
  }

  private static InternetAddress address(final String email, final String name)
      throws UnsupportedEncodingException {
    return new InternetAddress(email, name, StandardCharsets.UTF_8.name());
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
    return Retry.of("brevo-smtp-email", retryConfig);
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
    return CircuitBreaker.of("brevo-smtp-email", circuitConfig);
  }

  /**
   * Un fallo es transitorio cuando puede resolverse repitiendo el envio (problemas de red o errores
   * temporales del servidor SMTP). La autenticacion rechazada o un mensaje mal formado no se
   * reintentan.
   */
  private static boolean isTransientFailure(final Throwable exception) {
    if (exception instanceof MailAuthenticationException
        || exception instanceof MailParseException
        || exception instanceof MailPreparationException) {
      return false;
    }
    return exception instanceof MailSendException || exception instanceof MessagingException;
  }

  private static String providerDetail(final MailException exception) {
    return exception instanceof MailAuthenticationException
        ? AUTH_FAILURE_DETAIL
        : exception.getClass().getSimpleName();
  }

  private static String exceptionType(final Throwable exception) {
    return Objects.isNull(exception) ? "causa desconocida" : exception.getClass().getSimpleName();
  }
}
