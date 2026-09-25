package com.jcaa.usersmanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Habilita el envio asincrono y aporta el executor de la capa de aplicacion.
 *
 * <p>El bean se registra con el nombre {@code taskExecutor}, que es el que Spring resuelve por
 * defecto para {@code @Async}. Asi la capa de aplicacion solo necesita la anotacion y no depende
 * de ninguna constante de infraestructura.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncEmailConfig {

  private static final int CORE_POOL_SIZE = 2;
  private static final int MAX_POOL_SIZE = 4;
  private static final int QUEUE_CAPACITY = 200;
  private static final String THREAD_NAME_PREFIX = "email-notifier-";

  @Bean
  public TaskExecutor taskExecutor() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(CORE_POOL_SIZE);
    executor.setMaxPoolSize(MAX_POOL_SIZE);
    executor.setQueueCapacity(QUEUE_CAPACITY);
    executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
    executor.initialize();
    return executor;
  }
}
