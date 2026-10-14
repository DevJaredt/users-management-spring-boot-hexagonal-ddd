package com.jcaa.notifiers.infrastructure.adapter.kafka.dto;

/**
 * Resultado del envio que el servicio notifiers publica en {@code notification-status}
 * (paso 7 del diagrama).
 *
 * <p>No incluye el email del destinatario: solo identificadores tecnicos.
 */
public record NotificationStatusMessage(
    String eventId, String userId, NotificationStatus status, String detail, String occurredAt) {

  public enum NotificationStatus {
    SENT,
    FAILED,
    SKIPPED
  }
}
