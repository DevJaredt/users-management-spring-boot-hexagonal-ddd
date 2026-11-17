package com.jcaa.search.infrastructure.adapter.opensearch;

import com.jcaa.search.application.port.out.UserSearchIndexPort;
import com.jcaa.search.domain.model.UserSearchDocument;
import com.jcaa.search.domain.model.UserSearchResult;
import java.util.List;
import java.util.Optional;

/**
 * Implementación usada cuando OpenSearch no está configurado.
 *
 * <p>El servicio arranca y sigue consumiendo/ignorando eventos; los endpoints de búsqueda responden
 * 503 hasta configurar las credenciales.
 */
public class NoOpUserSearchIndexAdapter implements UserSearchIndexPort {

  @Override
  public Optional<UserSearchDocument> findById(final String userId) {
    return Optional.empty();
  }

  @Override
  public UserSearchResult search(final String text, final int page, final int size) {
    return new UserSearchResult(List.of(), 0);
  }

  @Override
  public void index(final UserSearchDocument document) {
    // Sin OpenSearch configurado no hay nada que indexar.
  }

  @Override
  public void delete(final String userId) {
    // Sin OpenSearch configurado no hay nada que eliminar.
  }

  @Override
  public boolean isAvailable() {
    return false;
  }
}
