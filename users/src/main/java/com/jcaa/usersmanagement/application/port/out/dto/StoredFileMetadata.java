package com.jcaa.usersmanagement.application.port.out.dto;

import java.time.Instant;

/**
 * Metadatos de un archivo almacenado en el bucket privado del proveedor de objetos.
 *
 * <p>No incluye el contenido: para descargarlo se usa {@link StoredFile}.
 */
public record StoredFileMetadata(
    String fileName, String contentType, long size, Instant lastModified) {}
