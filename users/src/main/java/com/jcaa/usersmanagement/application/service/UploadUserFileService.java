package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.UploadUserFileUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;
import com.jcaa.usersmanagement.domain.enums.UserFileContentType;
import com.jcaa.usersmanagement.domain.exception.InvalidUserFileException;
import com.jcaa.usersmanagement.domain.exception.UserFileStorageUnavailableException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.valueobject.UserFileName;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Sube una foto o PDF a la carpeta privada del usuario en Backblaze B2 (Guía 9).
 *
 * <p>Reglas: el usuario debe existir, el archivo no puede superar la política configurada y solo se
 * aceptan imágenes o PDF. El nombre se normaliza y el objeto se guarda bajo
 * {@code users/{userId}/{fileName}}.
 */
@Service
@RequiredArgsConstructor
public class UploadUserFileService implements UploadUserFileUseCase {

  private static final String CONTENT_TYPE_SEPARATOR = ";";
  private static final String EXTENSION_SEPARATOR = "\\.";
  private static final String GENERIC_CONTENT_TYPE = "application/octet-stream";

  private final UserFileStoragePort userFileStoragePort;
  private final GetUserByIdPort getUserByIdPort;
  private final UserFileUploadPolicy userFileUploadPolicy;
  private final Validator validator;

  @Override
  public StoredFileMetadata execute(final UploadUserFileCommand command) {
    validateCommand(command);

    final UserId ownerId = new UserId(command.userId());
    ensureUserExists(ownerId);
    ensureStorageIsAvailable();
    ensureContentIsValid(command.content());

    final UserFileName fileName = UserFileName.from(command.fileName());
    final UserFileContentType contentType =
        resolveContentType(command.contentType(), fileName.value());

    return userFileStoragePort.store(ownerId, fileName, command.content(), contentType);
  }

  private void validateCommand(final UploadUserFileCommand command) {
    final Set<ConstraintViolation<UploadUserFileCommand>> violations = validator.validate(command);
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

  private void ensureContentIsValid(final byte[] content) {
    if (content.length == 0) {
      throw InvalidUserFileException.becauseContentIsEmpty();
    }
    if (content.length > userFileUploadPolicy.maxBytes()) {
      throw InvalidUserFileException.becauseSizeExceedsLimit(userFileUploadPolicy.maxBytes());
    }
  }

  private UserFileContentType resolveContentType(
      final String declaredContentType, final String fileName) {
    final Optional<UserFileContentType> byExtension = fromExtension(fileName);
    if (Objects.isNull(declaredContentType) || declaredContentType.isBlank()) {
      return byExtension.orElseThrow(InvalidUserFileException::becauseContentTypeIsMissing);
    }

    final String normalized = declaredContentType.split(CONTENT_TYPE_SEPARATOR)[0].trim().toLowerCase(Locale.ROOT);
    final Optional<UserFileContentType> byMimeType =
        Arrays.stream(UserFileContentType.values())
            .filter(candidate -> candidate.mimeType().equals(normalized))
            .findFirst();
    if (byMimeType.isPresent()) {
      return byMimeType.get();
    }
    if (GENERIC_CONTENT_TYPE.equals(normalized) && byExtension.isPresent()) {
      return byExtension.get();
    }
    throw InvalidUserFileException.becauseContentTypeIsNotAllowed(declaredContentType);
  }

  private Optional<UserFileContentType> fromExtension(final String fileName) {
    final String[] segments = fileName.split(EXTENSION_SEPARATOR);
    if (segments.length < 2) {
      return Optional.empty();
    }
    final String extension = segments[segments.length - 1];
    return Arrays.stream(UserFileContentType.values())
        .filter(candidate -> candidate.supportsExtension(extension))
        .findFirst();
  }
}
