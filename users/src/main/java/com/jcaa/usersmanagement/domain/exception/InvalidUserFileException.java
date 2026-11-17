package com.jcaa.usersmanagement.domain.exception;

/** El archivo que se intenta subir no cumple las reglas del dominio. */
public final class InvalidUserFileException extends DomainException {

  private static final String MESSAGE_FILE_NAME_INVALID =
      "The file name is not valid. Use letters, numbers, dots, dashes or underscores.";
  private static final String MESSAGE_FILE_NAME_TOO_LONG =
      "The file name exceeds the maximum of %d characters.";
  private static final String MESSAGE_EMPTY_CONTENT = "The file content must not be empty.";
  private static final String MESSAGE_SIZE_EXCEEDED =
      "The file exceeds the maximum size of %d bytes.";
  private static final String MESSAGE_CONTENT_TYPE_NOT_ALLOWED =
      "The content type '%s' is not allowed. Only images and PDF files are accepted.";
  private static final String MESSAGE_CONTENT_TYPE_MISSING = "The file content type is required.";
  private static final String MESSAGE_CONTENT_UNREADABLE = "The file content could not be read.";

  private InvalidUserFileException(final String message) {
    super(message);
  }

  private InvalidUserFileException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static InvalidUserFileException becauseFileNameIsInvalid() {
    return new InvalidUserFileException(MESSAGE_FILE_NAME_INVALID);
  }

  public static InvalidUserFileException becauseFileNameIsTooLong(final int maximumLength) {
    return new InvalidUserFileException(
        String.format(MESSAGE_FILE_NAME_TOO_LONG, maximumLength));
  }

  public static InvalidUserFileException becauseContentIsEmpty() {
    return new InvalidUserFileException(MESSAGE_EMPTY_CONTENT);
  }

  public static InvalidUserFileException becauseSizeExceedsLimit(final long maximumBytes) {
    return new InvalidUserFileException(String.format(MESSAGE_SIZE_EXCEEDED, maximumBytes));
  }

  public static InvalidUserFileException becauseContentTypeIsNotAllowed(
      final String contentType) {
    return new InvalidUserFileException(
        String.format(MESSAGE_CONTENT_TYPE_NOT_ALLOWED, contentType));
  }

  public static InvalidUserFileException becauseContentTypeIsMissing() {
    return new InvalidUserFileException(MESSAGE_CONTENT_TYPE_MISSING);
  }

  public static InvalidUserFileException becauseContentCouldNotBeRead(final Throwable cause) {
    return new InvalidUserFileException(MESSAGE_CONTENT_UNREADABLE, cause);
  }
}
