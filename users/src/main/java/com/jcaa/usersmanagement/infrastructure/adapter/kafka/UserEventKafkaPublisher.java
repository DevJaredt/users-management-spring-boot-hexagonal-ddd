package com.jcaa.usersmanagement.infrastructure.adapter.kafka;

import com.jcaa.usersmanagement.application.port.out.UserEventPublisherPort;
import com.jcaa.usersmanagement.application.port.out.dto.UserEvent;
import com.jcaa.usersmanagement.infrastructure.adapter.kafka.dto.UserEventMessage;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica los cambios de usuario en el topico {@code user-events} (paso 2 del diagrama).
 *
 * <p>La publicacion es <b>no bloqueante</b>: {@code KafkaTemplate.send} es asincrono, asi que la
 * peticion HTTP no espera al broker. Si el broker no esta disponible se registra un aviso y la
 * operacion de negocio ya persistida no se revierte (se requeriria un patron outbox para
 * garantizar la entrega).
 */
@Slf4j
@Component
public class UserEventKafkaPublisher implements UserEventPublisherPort {

  private static final String LOG_PUBLISHED =
      "[UserEventKafkaPublisher] evento publicado. eventId={} tipo={} userId={}";
  private static final String LOG_PUBLISH_FAILED =
      "[UserEventKafkaPublisher] no se pudo publicar el evento. tipo={} userId={} causa={}";

  private final KafkaTemplate<String, UserEventMessage> kafkaTemplate;
  private final String topic;

  public UserEventKafkaPublisher(
      final KafkaTemplate<String, UserEventMessage> kafkaTemplate,
      @Value("${kafka.topic.user-events}") final String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  @Override
  public void publish(final UserEvent event) {
    final String eventId = UUID.randomUUID().toString();
    final UserEventMessage message =
        new UserEventMessage(
            eventId,
            event.type(),
            Instant.now().toString(),
            event.userId(),
            event.name(),
            event.email(),
            event.role(),
            event.status());
    try {
      kafkaTemplate.send(topic, event.userId(), message);
      log.info(LOG_PUBLISHED, eventId, event.type(), event.userId());
    } catch (final RuntimeException exception) {
      log.warn(LOG_PUBLISH_FAILED, event.type(), event.userId(), exception.getClass().getSimpleName());
    }
  }
}
