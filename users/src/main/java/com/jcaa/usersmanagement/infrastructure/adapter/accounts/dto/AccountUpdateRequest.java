package com.jcaa.usersmanagement.infrastructure.adapter.accounts.dto;

/** Cuerpo JSON que espera el servicio accounts para actualizar una cuenta. */
public record AccountUpdateRequest(String name, String email) {}
