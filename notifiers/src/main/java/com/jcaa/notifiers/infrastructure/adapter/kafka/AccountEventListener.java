package com.jcaa.notifiers.infrastructure.adapter.kafka;

import com.jcaa.notifiers.application.exception.EmailDeliveryException;
import com.jcaa.notifiers.application.service.UserNotificationService;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.AccountEventMessage;
import com.jcaa.notifiers.infrastructure.adapter.kafka.dto.NotificationStatusMessage.NotificationStatus;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topico {@code create-account} (pasos 3 y 4 del diagrama para la cuenta).
 *
 * <p>Cuando el servicio accounts persiste la cuenta publica este evento; aqui se envia el correo
 * de <b>alta</b> (usuario + cuenta asociada) y se registra el resultado en
 * {@code notification-status} (paso 7). Es el unico punto que notifica el alta: el evento
 * {@code create-user} no envia correo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventListener {

  private static final String LOG_RECEIVED =
      "[AccountEventListener] evento recibido. eventId={} tipo={} accountId={}";
  private static final String LOG_SENT = "[AccountEventListener] notificacion enviada. eventId={}";
  private static final String LOG_FAILED =
      "[AccountEventListener] notificacion fallida. eventId={} causa={}";
  private static final String DETAIL_MISSING_DATA = "evento sin email de destinatario";

  private final UserNotificationService userNotificationService;
  private final NotificationStatusPublisher statusPublisher;

  @KafkaListener(
      topics = "${kafka.topic.create-account}",
      groupId = "${spring.kafka.consumer.group-id}",
      properties = {
        "spring.json.value.default.type=com.jcaa.notifiers.infrastructure.adapter.kafka.dto.AccountEventMessage"
      })
  public void onAccountEvent(final AccountEventMessage event) {
    log.info(LOG_RECEIVED, event.eventId(), event.eventType(), event.accountId());

    if (isBlank(event.email()) || isBlank(event.eventId())) {
      log.warn(LOG_FAILED, event.eventId(), DETAIL_MISSING_DATA);
      statusPublisher.publish(
          event.eventId(), event.userId(), NotificationStatus.FAILED, DETAIL_MISSING_DATA);
      return;
    }

    try {
      userNotificationService.notifyAccountCreated(event.name(), event.email());
      log.info(LOG_SENT, event.eventId());
      statusPublisher.publish(event.eventId(), event.userId(), NotificationStatus.SENT, null);
    } catch (final EmailDeliveryException exception) {
      log.warn(LOG_FAILED, event.eventId(), exception.getClass().getSimpleName());
      statusPublisher.publish(
          event.eventId(), event.userId(), NotificationStatus.FAILED, exception.getMessage());
    }
  }

  private static boolean isBlank(final String value) {
    return Objects.isNull(value) || value.isBlank();
  }
}
