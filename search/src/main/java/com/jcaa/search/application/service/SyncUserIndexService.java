package com.jcaa.search.application.service;

import com.jcaa.search.application.port.in.SyncUserIndexUseCase;
import com.jcaa.search.application.port.out.UserSearchIndexPort;
import com.jcaa.search.application.service.dto.command.IndexUserCommand;
import com.jcaa.search.domain.model.UserSearchDocument;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Sincroniza el índice con los eventos de usuario (lado de escritura del CQRS).
 *
 * <p>La base de datos relacional sigue siendo la fuente de verdad; OpenSearch solo mantiene una
 * representación consultable. Por eso se acepta consistencia eventual.
 */
@Service
@RequiredArgsConstructor
public class SyncUserIndexService implements SyncUserIndexUseCase {

  private static final String ERROR_USER_ID = "The user id is required.";

  private final UserSearchIndexPort userSearchIndexPort;
  private final Validator validator;

  @Override
  public void index(final IndexUserCommand command) {
    validateCommand(command);
    userSearchIndexPort.index(
        new UserSearchDocument(
            command.id(), command.name(), command.email(), command.role(), command.status()));
  }

  @Override
  public void delete(final String userId) {
    if (Objects.isNull(userId) || userId.isBlank()) {
      throw new IllegalArgumentException(ERROR_USER_ID);
    }
    userSearchIndexPort.delete(userId.trim());
  }

  private void validateCommand(final IndexUserCommand command) {
    final Set<ConstraintViolation<IndexUserCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
