package com.jcaa.usersmanagement.application.port.out.dto;

/** Tipos de evento de usuario que el servicio users publica en el topico (paso 2 del diagrama). */
public enum UserEventType {
  USER_CREATED,
  USER_UPDATED,
  USER_DELETED
}
