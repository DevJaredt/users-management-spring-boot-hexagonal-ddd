package com.jcaa.search.infrastructure.entrypoint.rest.mapper;

import com.jcaa.search.application.service.dto.query.SearchUsersQuery;
import com.jcaa.search.domain.model.UserSearchDocument;
import com.jcaa.search.domain.model.UserSearchResult;
import com.jcaa.search.infrastructure.entrypoint.rest.dto.response.UserSearchPageRestResponse;
import com.jcaa.search.infrastructure.entrypoint.rest.dto.response.UserSearchRestResponse;
import java.util.List;
import lombok.experimental.UtilityClass;

/** Mapea entre el dominio de búsqueda y los DTOs REST. */
@UtilityClass
public class UserSearchRestMapper {

  public SearchUsersQuery toQuery(final String text, final int page, final int size) {
    return new SearchUsersQuery(text, page, size);
  }

  public UserSearchRestResponse toResponse(final UserSearchDocument document) {
    return new UserSearchRestResponse(
        document.id(), document.name(), document.email(), document.role(), document.status());
  }

  public UserSearchPageRestResponse toPageResponse(
      final UserSearchResult result, final int page, final int size) {
    final List<UserSearchRestResponse> users =
        result.users().stream().map(UserSearchRestMapper::toResponse).toList();
    return new UserSearchPageRestResponse(users, result.total(), page, size);
  }
}
