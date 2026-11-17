package com.jcaa.usersmanagement.application.service.dto.command;

import jakarta.validation.constraints.NotBlank;

/** Comando para eliminar un archivo de un usuario. */
public record DeleteUserFileCommand(
    @NotBlank(message = "userId must not be blank") String userId,
    @NotBlank(message = "fileName must not be blank") String fileName) {}
