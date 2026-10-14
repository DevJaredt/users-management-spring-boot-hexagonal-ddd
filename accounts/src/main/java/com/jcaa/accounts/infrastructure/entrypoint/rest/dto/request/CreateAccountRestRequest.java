package com.jcaa.accounts.infrastructure.entrypoint.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAccountRestRequest(
    @NotBlank(message = "userId must not be blank") String userId,
    String name,
    @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email) {}
