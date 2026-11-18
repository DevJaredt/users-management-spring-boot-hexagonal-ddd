package com.jcaa.notifiers.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Arranque tolerante a fallos de los consumidores de Kafka.
 *
 * <p>Si el broker no esta disponible cuando arranca el contenedor, iniciar los listeners durante
 * el refresco del contexto lanza una excepcion y tumba la aplicacion completa (crash loop en
 * Render). Con {@code spring.kafka.listener.auto-startup=false} el contexto arranca igual, el
 * endpoint de salud queda arriba y esta clase reintenta levantar cada listener en segundo plano
 * hasta que Kafka responda.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class KafkaListenerRecoveryConfig {

  private static final String LOG_STARTED = "[KafkaListenerRecovery] listener iniciado. id={}";
  private static final String LOG_RETRY =
      "[KafkaListenerRecovery] Kafka no disponible; se reintenta en {} ms. id={} causa={}";

  private final KafkaListenerEndpointRegistry registry;
  private final long retryDelayMs;

  public KafkaListenerRecoveryConfig(
      final KafkaListenerEndpointRegistry registry,
      @Value("${kafka.listener.retry-delay-ms:15000}") final long retryDelayMs) {
    this.registry = registry;
    this.retryDelayMs = retryDelayMs;
  }

  @Scheduled(
      initialDelayString = "${kafka.listener.initial-delay-ms:0}",
      fixedDelayString = "${kafka.listener.retry-delay-ms:15000}")
  public void startPendingListeners() {
    for (final MessageListenerContainer container : registry.getListenerContainers()) {
      if (!container.isRunning()) {
        start(container);
      }
    }
  }

  private void start(final MessageListenerContainer container) {
    try {
      container.start();
      log.info(LOG_STARTED, container.getListenerId());
    } catch (final RuntimeException exception) {
      log.warn(LOG_RETRY, retryDelayMs, container.getListenerId(), describe(exception));
    }
  }

  private static String describe(final Throwable throwable) {
    Throwable cause = throwable;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause.getClass().getSimpleName() + ": " + cause.getMessage();
  }
}
