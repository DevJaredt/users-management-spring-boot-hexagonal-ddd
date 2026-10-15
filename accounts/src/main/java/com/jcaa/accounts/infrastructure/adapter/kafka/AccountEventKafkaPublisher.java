package com.jcaa.accounts.infrastructure.adapter.kafka;

import com.jcaa.accounts.application.port.out.AccountEventPublisherPort;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.infrastructure.adapter.kafka.dto.AccountEventMessage;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica el evento de cuenta creada en el topico {@code create-account} (paso 7 del diagrama).
 *
 * <p>El mensaje viaja como <b>JSON</b> (Jackson). La publicacion es no bloqueante: si el broker no
 * esta disponible se registra un aviso y la cuenta ya persistida no se revierte.
 */
@Slf4j
@Component
public class AccountEventKafkaPublisher implements AccountEventPublisherPort {

  private static final String EVENT_TYPE_ACCOUNT_CREATED = "ACCOUNT_CREATED";
  private static final String LOG_PUBLISHED =
      "[AccountEventKafkaPublisher] evento publicado. eventId={} accountId={} topico={}";
  private static final String LOG_PUBLISH_FAILED =
      "[AccountEventKafkaPublisher] no se pudo publicar el evento. accountId={} causa={}";

  private final KafkaTemplate<String, AccountEventMessage> kafkaTemplate;
  private final String topic;

  public AccountEventKafkaPublisher(
      final KafkaTemplate<String, AccountEventMessage> kafkaTemplate,
      @Value("${kafka.topic.create-account}") final String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  @Override
  public void publish(final AccountModel account) {
    final String eventId = UUID.randomUUID().toString();
    final AccountEventMessage message =
        new AccountEventMessage(
            eventId,
            EVENT_TYPE_ACCOUNT_CREATED,
            Instant.now().toString(),
            account.id(),
            account.userId(),
            account.name(),
            account.email());
    try {
      kafkaTemplate.send(topic, account.userId(), message);
      log.info(LOG_PUBLISHED, eventId, account.id(), topic);
    } catch (final RuntimeException exception) {
      log.warn(LOG_PUBLISH_FAILED, account.id(), exception.getClass().getSimpleName());
    }
  }
}
