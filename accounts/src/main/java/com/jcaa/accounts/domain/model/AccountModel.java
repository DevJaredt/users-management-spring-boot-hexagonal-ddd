package com.jcaa.accounts.domain.model;

import java.util.Objects;

/** Cuenta asociada a un usuario del servicio users. */
public record AccountModel(String id, String userId, String name, String email) {

  private static final String ERROR_ID = "El id de la cuenta es requerido.";
  private static final String ERROR_USER_ID = "El id del usuario es requerido.";
  private static final String ERROR_EMAIL = "El email de la cuenta es requerido.";

  public AccountModel {
    requireNotBlank(id, ERROR_ID);
    requireNotBlank(userId, ERROR_USER_ID);
    requireNotBlank(email, ERROR_EMAIL);
  }

  private static void requireNotBlank(final String value, final String errorMessage) {
    Objects.requireNonNull(value, errorMessage);
    if (value.isBlank()) {
      throw new IllegalArgumentException(errorMessage);
    }
  }
}
