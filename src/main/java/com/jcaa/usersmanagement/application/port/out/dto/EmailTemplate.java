package com.jcaa.usersmanagement.application.port.out.dto;

/**
 * Tipo de notificacion por correo solicitado por la capa de aplicacion.
 *
 * <p>La aplicacion decide QUE notificar; la infraestructura decide COMO se renderiza y se envia.
 * Por eso este enumerado no conoce rutas de plantillas ni detalles de SMTP.
 */
public enum EmailTemplate {
  USER_CREATED,
  USER_UPDATED
}
