package com.jcaa.search.infrastructure.entrypoint.rest.dto.response;

/** Usuario devuelto por los endpoints de búsqueda. */
public record UserSearchRestResponse(
    String id, String name, String email, String role, String status) {}
