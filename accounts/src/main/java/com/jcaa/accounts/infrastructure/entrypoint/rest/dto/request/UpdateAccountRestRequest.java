package com.jcaa.accounts.infrastructure.entrypoint.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de la actualizacion de una cuenta ({@code PUT /api/accounts/{userId}}). */
public record UpdateAccountRestRequest(
    String name,
    @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email) {}
