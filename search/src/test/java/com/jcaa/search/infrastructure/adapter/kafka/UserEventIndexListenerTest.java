package com.jcaa.search.infrastructure.adapter.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.jcaa.search.application.port.in.SyncUserIndexUseCase;
import com.jcaa.search.application.service.dto.command.IndexUserCommand;
import com.jcaa.search.infrastructure.adapter.kafka.dto.UserEventMessage;
import com.jcaa.search.infrastructure.adapter.kafka.dto.UserEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests para UserEventIndexListener (Guía 11: sincronización por eventos). */
@DisplayName("UserEventIndexListener")
@ExtendWith(MockitoExtension.class)
class UserEventIndexListenerTest {

  @Mock private SyncUserIndexUseCase syncUserIndexUseCase;

  private UserEventIndexListener listener;

  @BeforeEach
  void setUp() {
    listener = new UserEventIndexListener(syncUserIndexUseCase);
  }

  @Test
  @DisplayName("onUserChanged() indexa un evento de actualización")
  void shouldIndexUpdatedUser() {
    // Arrange
    final UserEventMessage event =
        new UserEventMessage(
            "evt-1",
            UserEventType.USER_UPDATED,
            "2026-11-16T00:00:00Z",
            "u-1",
            "John",
            "john@example.com",
            "ADMIN",
            "ACTIVE");

    // Act
    listener.onUserChanged(event);

    // Assert
    verify(syncUserIndexUseCase)
        .index(new IndexUserCommand("u-1", "John", "john@example.com", "ADMIN", "ACTIVE"));
  }

  @Test
  @DisplayName("onUserDeleted() elimina el documento del índice")
  void shouldDeleteUserFromIndex() {
    // Arrange
    final UserEventMessage event =
        new UserEventMessage(
            "evt-2",
            UserEventType.USER_DELETED,
            "2026-11-16T00:00:00Z",
            "u-1",
            null,
            null,
            null,
            null);

    // Act
    listener.onUserDeleted(event);

    // Assert
    verify(syncUserIndexUseCase).delete("u-1");
  }

  @Test
  @DisplayName("onUserChanged() ignora eventos incompletos")
  void shouldIgnoreIncompleteEvent() {
    // Arrange
    final UserEventMessage event =
        new UserEventMessage(
            "evt-3", UserEventType.USER_CREATED, null, "u-1", null, null, null, null);

    // Act
    listener.onUserChanged(event);

    // Assert
    verifyNoInteractions(syncUserIndexUseCase);
  }
}
