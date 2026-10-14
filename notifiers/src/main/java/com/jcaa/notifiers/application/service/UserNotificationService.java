package com.jcaa.notifiers.application.service;

import com.jcaa.notifiers.application.port.out.EmailSenderPort;
import com.jcaa.notifiers.application.port.out.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Decide QUE notificar cuando llega un evento de usuario.
 *
 * <p>No conoce Kafka ni Brevo: compone el mensaje de negocio y delega en {@link EmailSenderPort}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

  private static final String SUBJECT_CREATED = "Tu cuenta ha sido creada — Gestion de Usuarios";
  private static final String SUBJECT_UPDATED =
      "Tu cuenta ha sido actualizada — Gestion de Usuarios";

  private static final String BODY_CREATED =
      "<html><body><h2>Hola, %s</h2>"
          + "<p>Tu cuenta ha sido creada exitosamente.</p>"
          + "<ul><li><strong>Email:</strong> %s</li>"
          + "<li><strong>Rol:</strong> %s</li></ul>"
          + "<p>Ya puedes iniciar sesion con tu correo y la contrasena que registraste.</p>"
          + "</body></html>";

  private static final String BODY_UPDATED =
      "<html><body><h2>Hola, %s</h2>"
          + "<p>Tu cuenta ha sido actualizada exitosamente.</p>"
          + "<ul><li><strong>Email:</strong> %s</li>"
          + "<li><strong>Rol:</strong> %s</li>"
          + "<li><strong>Estado:</strong> %s</li></ul>"
          + "</body></html>";

  private final EmailSenderPort emailSenderPort;

  public void notifyUserCreated(
      final String name, final String email, final String role) {
    send(
        new EmailMessage(
            email,
            name,
            SUBJECT_CREATED,
            String.format(BODY_CREATED, escape(name), escape(email), escape(role))));
  }

  public void notifyUserUpdated(
      final String name, final String email, final String role, final String status) {
    send(
        new EmailMessage(
            email,
            name,
            SUBJECT_UPDATED,
            String.format(BODY_UPDATED, escape(name), escape(email), escape(role), escape(status))));
  }

  private void send(final EmailMessage message) {
    emailSenderPort.send(message);
  }

  /** Escapa los valores del evento antes de interpolarlos en el HTML del correo. */
  private static String escape(final String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
