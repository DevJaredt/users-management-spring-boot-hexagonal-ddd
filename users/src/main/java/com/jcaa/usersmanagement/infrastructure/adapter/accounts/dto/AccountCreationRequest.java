package com.jcaa.usersmanagement.infrastructure.adapter.accounts.dto;

/** Cuerpo JSON que espera el servicio accounts para crear una cuenta. */
public record AccountCreationRequest(String userId, String name, String email) {}
