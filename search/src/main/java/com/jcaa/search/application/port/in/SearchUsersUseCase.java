package com.jcaa.search.application.port.in;

import com.jcaa.search.application.service.dto.query.SearchUsersQuery;
import com.jcaa.search.domain.model.UserSearchResult;

/** Caso de uso: buscar usuarios por texto libre (lado de lectura del CQRS). */
public interface SearchUsersUseCase {

  UserSearchResult execute(SearchUsersQuery query);
}
