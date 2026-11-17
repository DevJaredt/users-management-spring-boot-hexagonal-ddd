package com.jcaa.usersmanagement.domain.exception;

/** El almacenamiento de archivos no está configurado o no está disponible. */
public final class UserFileStorageUnavailableException extends DomainException {

  private static final String MESSAGE_NOT_CONFIGURED =
      "File storage is not configured. Set B2_ENABLED and the Backblaze B2 credentials.";

  private UserFileStorageUnavailableException(final String message) {
    super(message);
  }

  public static UserFileStorageUnavailableException becauseItIsNotConfigured() {
    return new UserFileStorageUnavailableException(MESSAGE_NOT_CONFIGURED);
  }
}
