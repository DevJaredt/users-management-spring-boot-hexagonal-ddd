package com.jcaa.search.domain.model;

import java.util.List;

/** Resultado paginado de una búsqueda en OpenSearch. */
public record UserSearchResult(List<UserSearchDocument> users, long total) {}
