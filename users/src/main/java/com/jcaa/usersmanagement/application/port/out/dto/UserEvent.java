package com.jcaa.usersmanagement.application.port.out.dto;

import java.util.Objects;

/**
 * Evento de negocio que describe un cambio de usuario.
 *
 * <p>La aplicacion decide QUE cambio ocurrio; la infraestructura decide como se publica.
 */
public record UserEvent(
    UserEventType type, String userId, String name, String email, String role, String status) {

  private static final String ERROR_TYPE = "El tipo de evento es requerido.";
  private static final String ERROR_USER_ID = "El id del usuario es requerido.";
  private static final String ERROR_EMAIL = "El email del usuario es requerido.";

  public UserEvent {
    Objects.requireNonNull(type, ERROR_TYPE);
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
