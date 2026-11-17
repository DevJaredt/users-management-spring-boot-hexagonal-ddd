package com.jcaa.search.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.jcaa.search.application.port.out.UserSearchIndexPort;
import com.jcaa.search.application.service.dto.command.IndexUserCommand;
import com.jcaa.search.domain.model.UserSearchDocument;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests para SyncUserIndexService (Guía 11: lado de escritura del CQRS). */
@DisplayName("SyncUserIndexService")
@ExtendWith(MockitoExtension.class)
class SyncUserIndexServiceTest {

  @Mock private UserSearchIndexPort userSearchIndexPort;

  private SyncUserIndexService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service = new SyncUserIndexService(userSearchIndexPort, validatorFactory.getValidator());
    }
  }

  @Test
  @DisplayName("index() traduce el comando a documento y lo envía al índice")
  void shouldIndexCommand() {
    // Arrange
    final IndexUserCommand command =
        new IndexUserCommand("u-1", "John", "john@example.com", "ADMIN", "ACTIVE");

    // Act
    service.index(command);

    // Assert
    verify(userSearchIndexPort)
        .index(new UserSearchDocument("u-1", "John", "john@example.com", "ADMIN", "ACTIVE"));
  }

  @Test
  @DisplayName("delete() elimina el documento por id")
  void shouldDeleteById() {
    // Act
    service.delete("u-1");

    // Assert
    verify(userSearchIndexPort).delete("u-1");
  }

  @Test
  @DisplayName("index() rechaza comandos incompletos")
  void shouldRejectIncompleteCommand() {
    // Arrange
    final IndexUserCommand command = new IndexUserCommand("", "", "", "", "");

    // Act & Assert
    assertThrows(ConstraintViolationException.class, () -> service.index(command));
    verifyNoInteractions(userSearchIndexPort);
  }
}
