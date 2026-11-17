package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response;

/** Metadatos de un archivo de usuario devueltos por la API REST. */
public record UserFileRestResponse(
    String fileName, String contentType, long size, String lastModified) {}
