package com.jcaa.notifiers.infrastructure.adapter.kafka.dto;

/**
 * Evento de usuario que viaja por el topico {@code user-events}.
 *
 * <p>Es el contrato entre el servicio <b>users</b> (productor) y <b>notifiers</b> (consumidor).
 * Corresponde al paso 2 y 3 del diagrama de arquitectura.
 */
public record UserEventMessage(
    String eventId,
    UserEventType eventType,
    String occurredAt,
    String userId,
    String name,
    String email,
    String role,
    String status) {}
