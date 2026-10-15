package com.jcaa.notifiers.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;

/**
 * Crea los topicos automaticamente cuando el broker lo permite (desarrollo local).
 *
 * <p>Con Aiven puede que los permisos no permitan crearlos desde la aplicacion: en ese caso se
 * pasan a {@code KAFKA_TOPICS_AUTO_CREATE=false} y se crean en la consola de Aiven.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = "kafka.topics.auto-create",
    havingValue = "true",
    matchIfMissing = true)
public class KafkaTopicsConfig {

  private static final int PARTITIONS = 3;
  private static final int REPLICAS = 1;

  @Bean
  public NewTopics notifiersTopics(
      @Value("${kafka.topic.create-user}") final String createUserTopic,
      @Value("${kafka.topic.update-user}") final String updateUserTopic,
      @Value("${kafka.topic.create-account}") final String createAccountTopic,
      @Value("${kafka.topic.notification-status}") final String notificationStatusTopic) {
    return new NewTopics(
        TopicBuilder.name(createUserTopic).partitions(PARTITIONS).replicas(REPLICAS).build(),
        TopicBuilder.name(updateUserTopic).partitions(PARTITIONS).replicas(REPLICAS).build(),
        TopicBuilder.name(createAccountTopic).partitions(PARTITIONS).replicas(REPLICAS).build(),
        TopicBuilder
            .name(notificationStatusTopic)
            .partitions(PARTITIONS)
            .replicas(REPLICAS)
            .build());
  }
}
