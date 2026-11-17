package com.jcaa.usersmanagement.application.port.out.dto;

/** Contenido binario de un archivo junto con sus metadatos. */
public record StoredFile(StoredFileMetadata metadata, byte[] content) {}
