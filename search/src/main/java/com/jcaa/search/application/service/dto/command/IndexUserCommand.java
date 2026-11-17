package com.jcaa.search.application.service.dto.command;

import jakarta.validation.constraints.NotBlank;

/** Datos mínimos de un usuario para indexarlo en OpenSearch. */
public record IndexUserCommand(
    @NotBlank(message = "id must not be blank") String id,
    @NotBlank(message = "name must not be blank") String name,
    @NotBlank(message = "email must not be blank") String email,
    @NotBlank(message = "role must not be blank") String role,
    @NotBlank(message = "status must not be blank") String status) {}
