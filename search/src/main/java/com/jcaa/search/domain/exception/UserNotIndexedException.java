package com.jcaa.search.domain.exception;

/** El usuario solicitado todavía no está en el índice de búsqueda. */
public final class UserNotIndexedException extends RuntimeException {

  private static final String MESSAGE = "The user with id '%s' is not indexed yet.";

  private UserNotIndexedException(final String message) {
    super(message);
  }

  public static UserNotIndexedException becauseIdIsNotIndexed(final String userId) {
    return new UserNotIndexedException(String.format(MESSAGE, userId));
  }
}
