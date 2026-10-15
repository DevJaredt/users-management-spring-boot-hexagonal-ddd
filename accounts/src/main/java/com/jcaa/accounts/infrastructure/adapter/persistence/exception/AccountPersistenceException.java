package com.jcaa.accounts.infrastructure.adapter.persistence.exception;

public class AccountPersistenceException extends RuntimeException {

  public AccountPersistenceException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
