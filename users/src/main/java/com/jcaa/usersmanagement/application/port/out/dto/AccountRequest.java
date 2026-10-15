package com.jcaa.usersmanagement.application.port.out.dto;

/**
 * Datos minimos para crear la cuenta asociada a un usuario (paso 5 del diagrama).
 */
public record AccountRequest(String userId, String name, String email) {

  private static final String ERROR_USER_ID = "El id del usuario es requerido.";
  private static final String ERROR_EMAIL = "El email del usuario es requerido.";

  public AccountRequest {
    requireNotBlank(userId, ERROR_USER_ID);
    requireNotBlank(email, ERROR_EMAIL);
  }

  private static void requireNotBlank(final String value, final String errorMessage) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(errorMessage);
    }
  }
}
