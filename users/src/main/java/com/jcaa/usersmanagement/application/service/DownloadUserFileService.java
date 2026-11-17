package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.DownloadUserFileUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFile;
import com.jcaa.usersmanagement.application.service.dto.query.DownloadUserFileQuery;
import com.jcaa.usersmanagement.domain.exception.UserFileNotFoundException;
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

/** Descarga un archivo del bucket privado validando primero que el usuario exista. */
@Service
@RequiredArgsConstructor
public class DownloadUserFileService implements DownloadUserFileUseCase {

  private final UserFileStoragePort userFileStoragePort;
  private final GetUserByIdPort getUserByIdPort;
  private final Validator validator;

  @Override
  public StoredFile execute(final DownloadUserFileQuery query) {
    validateQuery(query);

    final UserId ownerId = new UserId(query.userId());
    ensureUserExists(ownerId);
    ensureStorageIsAvailable();

    final UserFileName fileName = UserFileName.from(query.fileName());
    return userFileStoragePort
        .load(ownerId, fileName)
        .orElseThrow(() -> UserFileNotFoundException.becauseFileNameWasNotFound(fileName.value()));
  }

  private void validateQuery(final DownloadUserFileQuery query) {
    final Set<ConstraintViolation<DownloadUserFileQuery>> violations = validator.validate(query);
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
