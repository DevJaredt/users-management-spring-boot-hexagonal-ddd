package com.jcaa.search.infrastructure.entrypoint.rest.dto.response;

/** Error uniforme de la API de búsqueda. */
public record ApiErrorResponse(int status, String message) {}
