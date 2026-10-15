package com.jcaa.notifiers.infrastructure.adapter.kafka;

import com.jcaa.notifiers.application.exception.EmailDeliveryException;
import com.jcaa.notifiers.application.service.UserNotificationService;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.NotificationStatusMessage.NotificationStatus;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.UserEventMessage;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topico {@code update-user} (pasos 3 y 4 del diagrama).
 *
 * <p>Traduce el evento a una notificacion de negocio ({@link UserNotificationService}) y publica
 * el resultado en {@code notification-status} (paso 7). El <b>alta</b> no se notifica aqui: la
 * dispara el evento {@code create-account} (ver {@link AccountEventListener}). Los datos personales
 * nunca se registran en los logs: solo identificadores tecnicos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

  private static final String LOG_RECEIVED =
      "[UserEventListener] evento recibido. eventId={} tipo={} userId={}";
  private static final String LOG_SENT = "[UserEventListener] notificacion enviada. eventId={}";
  private static final String LOG_FAILED =
      "[UserEventListener] notificacion fallida. eventId={} causa={}";
  private static final String LOG_SKIPPED =
      "[UserEventListener] evento sin notificacion asociada. eventId={} tipo={}";
  private static final String DETAIL_MISSING_DATA = "evento sin email de destinatario";

  private final UserNotificationService userNotificationService;
  private final NotificationStatusPublisher statusPublisher;

  @KafkaListener(
      topics = "${kafka.topic.update-user}",
      groupId = "${spring.kafka.consumer.group-id}",
      properties = {
        "spring.json.value.default.type=com.jcaa.notifiers.infrastructure.adapter.kafka.dto.UserEventMessage"
      })
  public void onUserEvent(final UserEventMessage event) {
    log.info(LOG_RECEIVED, event.eventId(), event.eventType(), event.userId());

    if (isBlank(event.email()) || isBlank(event.eventId())) {
      log.warn(LOG_FAILED, event.eventId(), DETAIL_MISSING_DATA);
      statusPublisher.publish(
          event.eventId(), event.userId(), NotificationStatus.FAILED, DETAIL_MISSING_DATA);
      return;
    }

    try {
      final NotificationStatus status = dispatch(event);
      if (NotificationStatus.SENT == status) {
        log.info(LOG_SENT, event.eventId());
      }
      statusPublisher.publish(event.eventId(), event.userId(), status, null);
    } catch (final EmailDeliveryException exception) {
      log.warn(LOG_FAILED, event.eventId(), exception.getClass().getSimpleName());
      statusPublisher.publish(
          event.eventId(), event.userId(), NotificationStatus.FAILED, exception.getMessage());
    }
  }

  private NotificationStatus dispatch(final UserEventMessage event) {
    switch (event.eventType()) {
      case USER_UPDATED -> {
        userNotificationService.notifyUserUpdated(
            event.name(), event.email(), event.role(), event.status());
        return NotificationStatus.SENT;
      }
      default -> {
        log.info(LOG_SKIPPED, event.eventId(), event.eventType());
        return NotificationStatus.SKIPPED;
      }
    }
  }

  private static boolean isBlank(final String value) {
    return Objects.isNull(value) || value.isBlank();
  }
}
