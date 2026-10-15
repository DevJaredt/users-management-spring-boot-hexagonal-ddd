package com.jcaa.notifiers.application.service;

import com.jcaa.notifiers.application.port.out.EmailSenderPort;
import com.jcaa.notifiers.application.port.out.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Decide QUE notificar cuando llega un evento.
 *
 * <p>No conoce Kafka ni Brevo: compone el mensaje de negocio y delega en {@link EmailSenderPort}.
 *
 * <p>El alta se notifica <b>una sola vez</b>, a partir del evento {@code create-account} (cuando la
 * cuenta ya quedo persistida); el evento {@code create-user} no envia correo por si mismo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

  private static final String SUBJECT_UPDATED =
      "Tu cuenta ha sido actualizada — Gestion de Usuarios";
  private static final String SUBJECT_ACCOUNT_CREATED =
      "Tu cuenta ha sido creada — Gestion de Usuarios";

  private static final String BODY_ACCOUNT_CREATED =
      "<html><body><h2>Hola, %s</h2>"
          + "<p>Tu usuario fue creado exitosamente y la cuenta asociada a el tambien quedo"
          + " registrada.</p>"
          + "<ul><li><strong>Email:</strong> %s</li></ul>"
          + "</body></html>";

  private static final String BODY_UPDATED =
      "<html><body><h2>Hola, %s</h2>"
          + "<p>Tu cuenta ha sido actualizada exitosamente.</p>"
          + "<ul><li><strong>Email:</strong> %s</li>"
          + "<li><strong>Rol:</strong> %s</li>"
          + "<li><strong>Estado:</strong> %s</li></ul>"
          + "</body></html>";

  private static final String SUBJECT_TEST = "Prueba de notificaciones — Gestion de Usuarios";
  private static final String BODY_TEST =
      "<html><body><h2>Hola, %s</h2>"
          + "<p>Este es un correo de prueba del microservicio de notificaciones.</p>"
          + "</body></html>";

  private final EmailSenderPort emailSenderPort;

  /** Correo de alta: se envia cuando {@code accounts} confirma que la cuenta quedo persistida. */
  public void notifyAccountCreated(final String name, final String email) {
    send(
        new EmailMessage(
            email,
            name,
            SUBJECT_ACCOUNT_CREATED,
            String.format(BODY_ACCOUNT_CREATED, escape(name), escape(email))));
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

  /**
   * Correo de prueba: permite validar la conexion SMTP (Brevo) sin pasar por Kafka. Lo usa el
   * endpoint {@code POST /api/notifications/test-email}.
   */
  public void notifyTest(final String name, final String email) {
    final String recipientName = (name == null || name.isBlank()) ? "equipo" : name;
    send(
        new EmailMessage(
            email,
            recipientName,
            SUBJECT_TEST,
            String.format(BODY_TEST, escape(recipientName))));
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
