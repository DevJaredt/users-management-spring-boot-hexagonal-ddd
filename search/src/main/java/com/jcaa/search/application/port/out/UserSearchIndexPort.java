package com.jcaa.search.application.port.out;

import com.jcaa.search.domain.model.UserSearchDocument;
import com.jcaa.search.domain.model.UserSearchResult;
import java.util.Optional;

/**
 * Puerto de salida hacia el índice de búsqueda (OpenSearch).
 *
 * <p>El lado de escritura del CQRS solo indexa o elimina documentos; el lado de lectura consulta el
 * índice y no la base de datos relacional.
 */
public interface UserSearchIndexPort {

  Optional<UserSearchDocument> findById(String userId);

  UserSearchResult search(String text, int page, int size);

  void index(UserSearchDocument document);

  void delete(String userId);

  boolean isAvailable();
}
