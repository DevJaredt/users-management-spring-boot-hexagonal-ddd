package com.jcaa.search.domain.exception;

/** El motor de búsqueda no está configurado o no está disponible. */
public final class SearchUnavailableException extends RuntimeException {

  private static final String MESSAGE =
      "Search is not available. Set OPENSEARCH_ENABLED and the OpenSearch credentials.";

  public SearchUnavailableException() {
    super(MESSAGE);
  }
}
