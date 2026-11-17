package com.jcaa.search.application.service;

import com.jcaa.search.application.port.in.GetIndexedUserUseCase;
import com.jcaa.search.application.port.out.UserSearchIndexPort;
import com.jcaa.search.domain.exception.SearchUnavailableException;
import com.jcaa.search.domain.exception.UserNotIndexedException;
import com.jcaa.search.domain.model.UserSearchDocument;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Obtiene un usuario desde el índice (lado de lectura del CQRS). */
@Service
@RequiredArgsConstructor
public class GetIndexedUserService implements GetIndexedUserUseCase {

  private static final String ERROR_USER_ID = "The user id is required.";

  private final UserSearchIndexPort userSearchIndexPort;

  @Override
  public UserSearchDocument execute(final String userId) {
    if (Objects.isNull(userId) || userId.isBlank()) {
      throw new IllegalArgumentException(ERROR_USER_ID);
    }
    if (!userSearchIndexPort.isAvailable()) {
      throw new SearchUnavailableException();
    }
    return userSearchIndexPort
        .findById(userId.trim())
        .orElseThrow(() -> UserNotIndexedException.becauseIdIsNotIndexed(userId));
  }
}
