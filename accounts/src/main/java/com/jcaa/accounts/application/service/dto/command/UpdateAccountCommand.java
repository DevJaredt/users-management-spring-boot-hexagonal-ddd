package com.jcaa.accounts.application.service.dto.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Comando de actualizacion de cuenta, ya validado por la capa de aplicacion. */
public record UpdateAccountCommand(
    @NotBlank(message = "userId must not be blank") String userId,
    String name,
    @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email) {}
