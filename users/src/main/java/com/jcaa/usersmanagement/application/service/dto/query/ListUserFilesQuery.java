package com.jcaa.usersmanagement.application.service.dto.query;

import jakarta.validation.constraints.NotBlank;

/** Consulta para listar los archivos de un usuario. */
public record ListUserFilesQuery(
    @NotBlank(message = "userId must not be blank") String userId) {}
