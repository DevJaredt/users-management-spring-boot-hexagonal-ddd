package com.jcaa.usersmanagement.application.service.dto.query;

import jakarta.validation.constraints.NotBlank;

/** Consulta para descargar un archivo concreto de un usuario. */
public record DownloadUserFileQuery(
    @NotBlank(message = "userId must not be blank") String userId,
    @NotBlank(message = "fileName must not be blank") String fileName) {}
