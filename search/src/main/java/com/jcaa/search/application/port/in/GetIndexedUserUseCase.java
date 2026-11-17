package com.jcaa.search.application.port.in;

import com.jcaa.search.domain.model.UserSearchDocument;

/** Caso de uso: obtener un usuario indexado. */
public interface GetIndexedUserUseCase {

  UserSearchDocument execute(String userId);
}
