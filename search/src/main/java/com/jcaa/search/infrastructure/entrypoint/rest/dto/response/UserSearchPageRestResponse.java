package com.jcaa.search.infrastructure.entrypoint.rest.dto.response;

import java.util.List;

/** Página de resultados de búsqueda. */
public record UserSearchPageRestResponse(
    List<UserSearchRestResponse> users, long total, int page, int size) {}
