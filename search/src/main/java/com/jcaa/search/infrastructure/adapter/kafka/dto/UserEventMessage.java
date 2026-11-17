package com.jcaa.search.infrastructure.adapter.kafka.dto;

/**
 * Evento de usuario tal como viaja por los tópicos {@code create-user}, {@code update-user} y
 * {@code delete-user}.
 *
 * <p>Contrato entre el servicio <b>users</b> (productor) y <b>search</b> (consumidor). No incluye
 * contraseña ni otros datos sensibles.
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
