package com.jcaa.notifiers.infrastructure.adapter.kafka;

import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.NotificationStatusMessage;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.NotificationStatusMessage.NotificationStatus;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.UserEventMessage;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica el resultado del envio en {@code notification-status} (paso 7 del diagrama).
 */
@Slf4j
@Component
public class NotificationStatusPublisher {

  private static final String LOG_PUBLISHED =
      "[NotificationStatusPublisher] estado publicado. eventId={} estado={}";
  private static final String LOG_PUBLISH_FAILED =
      "[NotificationStatusPublisher] no se pudo publicar el estado. eventId={} causa={}";

  private final KafkaTemplate<String, NotificationStatusMessage> kafkaTemplate;
  private final String topic;

  public NotificationStatusPublisher(
      final KafkaTemplate<String, NotificationStatusMessage> kafkaTemplate,
      @Value("${kafka.topic.notification-status}") final String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  public void publish(
      final UserEventMessage event, final NotificationStatus status, final String detail) {
    final NotificationStatusMessage message =
        new NotificationStatusMessage(
            event.eventId(), event.userId(), status, detail, Instant.now().toString());
    try {
      kafkaTemplate.send(topic, event.userId(), message);
      log.info(LOG_PUBLISHED, event.eventId(), status);
    } catch (final RuntimeException exception) {
      // Publicar el estado es informativo: no debe tumbar el consumo del evento.
      log.warn(LOG_PUBLISH_FAILED, event.eventId(), exception.getClass().getSimpleName());
    }
  }
}
