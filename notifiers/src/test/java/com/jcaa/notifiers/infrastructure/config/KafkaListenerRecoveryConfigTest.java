package com.jcaa.notifiers.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

@DisplayName("KafkaListenerRecoveryConfig")
@ExtendWith(MockitoExtension.class)
class KafkaListenerRecoveryConfigTest {

  private static final long RETRY_DELAY_MS = 15000L;
  private static final String LISTENER_ID = "account-listener";

  @Mock private KafkaListenerEndpointRegistry registry;
  @Mock private MessageListenerContainer container;

  private KafkaListenerRecoveryConfig config;

  @BeforeEach
  void setUp() {
    config = new KafkaListenerRecoveryConfig(registry, RETRY_DELAY_MS);
  }

  @Test
  @DisplayName("inicia los listeners que aun no estan corriendo")
  void shouldStartPendingListeners() {
    // Arrange
    when(registry.getListenerContainers()).thenReturn(List.of(container));
    when(container.isRunning()).thenReturn(false);
    when(container.getListenerId()).thenReturn(LISTENER_ID);

    // Act
    config.startPendingListeners();

    // Assert
    verify(container).start();
  }

  @Test
  @DisplayName("no reinicia los listeners que ya estan corriendo")
  void shouldSkipRunningListeners() {
    // Arrange
    when(registry.getListenerContainers()).thenReturn(List.of(container));
    when(container.isRunning()).thenReturn(true);

    // Act
    config.startPendingListeners();

    // Assert
    verify(container, never()).start();
  }

  @Test
  @DisplayName("si Kafka sigue caido registra el fallo sin propagar la excepcion")
  void shouldKeepApplicationAliveWhenBrokerIsDown() {
    // Arrange
    when(registry.getListenerContainers()).thenReturn(List.of(container));
    when(container.isRunning()).thenReturn(false);
    when(container.getListenerId()).thenReturn(LISTENER_ID);
    doThrow(new KafkaException("Failed to construct kafka consumer")).when(container).start();

    // Act + Assert
    assertDoesNotThrow(config::startPendingListeners);
  }
}
