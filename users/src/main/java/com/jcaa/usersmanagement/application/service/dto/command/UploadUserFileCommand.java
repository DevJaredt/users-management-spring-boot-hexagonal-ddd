package com.jcaa.usersmanagement.application.service.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Comando para subir un archivo de usuario.
 *
 * <p>{@code contentType} puede venir vacío: en ese caso se deduce de la extensión del archivo. El
 * tamaño y el tipo permitido se validan en el servicio con la política configurada.
 */
public record UploadUserFileCommand(
    @NotBlank(message = "userId must not be blank") String userId,
    @NotBlank(message = "fileName must not be blank") String fileName,
    String contentType,
    @NotNull(message = "content must not be null") byte[] content) {}
