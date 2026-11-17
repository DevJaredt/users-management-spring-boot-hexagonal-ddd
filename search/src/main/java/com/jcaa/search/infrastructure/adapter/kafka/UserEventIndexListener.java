package com.jcaa.search.infrastructure.adapter.kafka;

import com.jcaa.search.application.port.in.SyncUserIndexUseCase;
import com.jcaa.search.application.service.dto.command.IndexUserCommand;
import com.jcaa.search.infrastructure.adapter.kafka.dto.UserEventMessage;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos de usuario (Guía 11, lado de escritura del CQRS).
 *
 * <p>{@code create-user} y {@code update-user} indexan el documento; {@code delete-user} lo elimina.
 * Los datos personales nunca se registran en los logs: solo identificadores técnicos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventIndexListener {

  private static final String LISTENER_PROPERTIES =
      "spring.json.value.default.type=com.jcaa.search.infrastructure.adapter.kafka.dto.UserEventMessage";
  private static final String LOG_RECEIVED =
      "[UserEventIndexListener] evento recibido. eventId={} tipo={} userId={}";
  private static final String LOG_INDEXED =
      "[UserEventIndexListener] índice sincronizado. eventId={} userId={}";
  private static final String LOG_DELETED =
      "[UserEventIndexListener] documento eliminado del índice. eventId={} userId={}";
  private static final String LOG_SKIPPED =
      "[UserEventIndexListener] evento inválido o sin efecto. eventId={} tipo={}";
  private static final String LOG_FAILED =
      "[UserEventIndexListener] no se pudo sincronizar el índice. eventId={} causa={}";

  private final SyncUserIndexUseCase syncUserIndexUseCase;

  @KafkaListener(
      topics = {"${kafka.topic.create-user}", "${kafka.topic.update-user}"},
      groupId = "${spring.kafka.consumer.group-id}",
      properties = {LISTENER_PROPERTIES})
  public void onUserChanged(final UserEventMessage event) {
    log.info(LOG_RECEIVED, event.eventId(), event.eventType(), event.userId());
    if (hasMissingData(event)) {
      log.warn(LOG_SKIPPED, event.eventId(), event.eventType());
      return;
    }
    try {
      syncUserIndexUseCase.index(
          new IndexUserCommand(
              event.userId(), event.name(), event.email(), event.role(), event.status()));
      log.info(LOG_INDEXED, event.eventId(), event.userId());
    } catch (final RuntimeException exception) {
      log.warn(LOG_FAILED, event.eventId(), exception.getClass().getSimpleName());
    }
  }

  @KafkaListener(
      topics = "${kafka.topic.delete-user}",
      groupId = "${spring.kafka.consumer.group-id}",
      properties = {LISTENER_PROPERTIES})
  public void onUserDeleted(final UserEventMessage event) {
    log.info(LOG_RECEIVED, event.eventId(), event.eventType(), event.userId());
    if (Objects.isNull(event.userId()) || event.userId().isBlank()) {
      log.warn(LOG_SKIPPED, event.eventId(), event.eventType());
      return;
    }
    try {
      syncUserIndexUseCase.delete(event.userId());
      log.info(LOG_DELETED, event.eventId(), event.userId());
    } catch (final RuntimeException exception) {
      log.warn(LOG_FAILED, event.eventId(), exception.getClass().getSimpleName());
    }
  }

  private boolean hasMissingData(final UserEventMessage event) {
    return isBlank(event.userId())
        || isBlank(event.name())
        || isBlank(event.email())
        || isBlank(event.role())
        || isBlank(event.status());
  }

  private boolean isBlank(final String value) {
    return Objects.isNull(value) || value.isBlank();
  }
}
