package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.DeleteUserFileUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserFileCommand;
import com.jcaa.usersmanagement.domain.exception.UserFileStorageUnavailableException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.valueobject.UserFileName;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Elimina un archivo de la carpeta privada del usuario. */
@Service
@RequiredArgsConstructor
public class DeleteUserFileService implements DeleteUserFileUseCase {

  private final UserFileStoragePort userFileStoragePort;
  private final GetUserByIdPort getUserByIdPort;
  private final Validator validator;

  @Override
  public void execute(final DeleteUserFileCommand command) {
    validateCommand(command);

    final UserId ownerId = new UserId(command.userId());
    ensureUserExists(ownerId);
    ensureStorageIsAvailable();

    userFileStoragePort.delete(ownerId, UserFileName.from(command.fileName()));
  }

  private void validateCommand(final DeleteUserFileCommand command) {
    final Set<ConstraintViolation<DeleteUserFileCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private void ensureUserExists(final UserId userId) {
    getUserByIdPort
        .getById(userId)
        .orElseThrow(() -> UserNotFoundException.becauseIdWasNotFound(userId.value()));
  }

  private void ensureStorageIsAvailable() {
    if (!userFileStoragePort.isAvailable()) {
      throw UserFileStorageUnavailableException.becauseItIsNotConfigured();
    }
  }
}
