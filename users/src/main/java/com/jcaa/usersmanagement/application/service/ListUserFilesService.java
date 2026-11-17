package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.ListUserFilesUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.application.service.dto.query.ListUserFilesQuery;
import com.jcaa.usersmanagement.domain.exception.UserFileStorageUnavailableException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Lista los archivos que un usuario tiene en su carpeta del bucket privado. */
@Service
@RequiredArgsConstructor
public class ListUserFilesService implements ListUserFilesUseCase {

  private final UserFileStoragePort userFileStoragePort;
  private final GetUserByIdPort getUserByIdPort;
  private final Validator validator;

  @Override
  public List<StoredFileMetadata> execute(final ListUserFilesQuery query) {
    validateQuery(query);

    final UserId ownerId = new UserId(query.userId());
    ensureUserExists(ownerId);
    ensureStorageIsAvailable();

    return userFileStoragePort.list(ownerId);
  }

  private void validateQuery(final ListUserFilesQuery query) {
    final Set<ConstraintViolation<ListUserFilesQuery>> violations = validator.validate(query);
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
