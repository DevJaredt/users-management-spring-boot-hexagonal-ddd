package com.jcaa.search.application.service;

import com.jcaa.search.application.port.in.SearchUsersUseCase;
import com.jcaa.search.application.port.out.UserSearchIndexPort;
import com.jcaa.search.application.service.dto.query.SearchUsersQuery;
import com.jcaa.search.domain.exception.SearchUnavailableException;
import com.jcaa.search.domain.model.UserSearchResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Búsqueda de usuarios directamente en OpenSearch (lado de lectura del CQRS). */
@Service
@RequiredArgsConstructor
public class SearchUsersService implements SearchUsersUseCase {

  private final UserSearchIndexPort userSearchIndexPort;
  private final Validator validator;

  @Override
  public UserSearchResult execute(final SearchUsersQuery query) {
    validateQuery(query);
    ensureSearchIsAvailable();

    final String text = Objects.isNull(query.text()) ? "" : query.text().trim();
    return userSearchIndexPort.search(text, query.page(), query.size());
  }

  private void validateQuery(final SearchUsersQuery query) {
    final Set<ConstraintViolation<SearchUsersQuery>> violations = validator.validate(query);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private void ensureSearchIsAvailable() {
    if (!userSearchIndexPort.isAvailable()) {
      throw new SearchUnavailableException();
    }
  }
}
