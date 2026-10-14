package com.jcaa.accounts.domain.exception;

public class AccountNotFoundException extends RuntimeException {

  private static final String MESSAGE_WITH_ID =
      "No existe una cuenta asociada al usuario '%s'.";

  public AccountNotFoundException(final String message) {
    super(message);
  }

  public static AccountNotFoundException becauseUserIdWasNotFound(final String userId) {
    return new AccountNotFoundException(String.format(MESSAGE_WITH_ID, userId));
  }
}
