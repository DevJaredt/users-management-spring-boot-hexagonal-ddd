package com.jcaa.usersmanagement.domain.exception;

/** El archivo solicitado no existe en el almacenamiento del usuario. */
public final class UserFileNotFoundException extends DomainException {

  private static final String MESSAGE_BY_NAME = "The file '%s' was not found for the user.";

  private UserFileNotFoundException(final String message) {
    super(message);
  }

  public static UserFileNotFoundException becauseFileNameWasNotFound(final String fileName) {
    return new UserFileNotFoundException(String.format(MESSAGE_BY_NAME, fileName));
  }
}
