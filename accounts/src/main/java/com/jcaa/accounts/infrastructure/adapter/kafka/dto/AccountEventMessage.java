package com.jcaa.accounts.infrastructure.adapter.kafka.dto;

/**
 * Evento de cuenta que viaja por el topico {@code create-account}.
 *
 * <p>Contrato entre el servicio <b>accounts</b> (productor) y <b>notifiers</b> (consumidor). Se
 * serializa como JSON.
 */
public record AccountEventMessage(
    String eventId,
    String eventType,
    String occurredAt,
    String accountId,
    String userId,
    String name,
    String email) {}
