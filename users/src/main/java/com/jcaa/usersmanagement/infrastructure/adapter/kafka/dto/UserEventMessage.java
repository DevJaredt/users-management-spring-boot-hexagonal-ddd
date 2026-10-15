package com.jcaa.usersmanagement.infrastructure.adapter.kafka.dto;

import com.jcaa.usersmanagement.application.port.out.dto.UserEventType;

/**
 * Evento de usuario tal como viaja por los topicos {@code create-user} y {@code update-user}.
 *
 * <p>Contrato entre el servicio <b>users</b> (productor) y <b>notifiers</b> (consumidor).
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
