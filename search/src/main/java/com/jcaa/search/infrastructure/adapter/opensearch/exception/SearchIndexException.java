package com.jcaa.search.infrastructure.adapter.opensearch.exception;

/** Falla al comunicarse con el índice de OpenSearch. */
public final class SearchIndexException extends RuntimeException {

  private static final String MESSAGE_INDEX = "No se pudo indexar el usuario '%s'.";
  private static final String MESSAGE_DELETE = "No se pudo eliminar el usuario '%s' del índice.";
  private static final String MESSAGE_SEARCH = "No se pudo ejecutar la búsqueda.";
  private static final String MESSAGE_FIND = "No se pudo consultar el usuario '%s' en el índice.";

  private SearchIndexException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static SearchIndexException becauseIndexFailed(
      final String userId, final Throwable cause) {
    return new SearchIndexException(String.format(MESSAGE_INDEX, userId), cause);
  }

  public static SearchIndexException becauseDeleteFailed(
      final String userId, final Throwable cause) {
    return new SearchIndexException(String.format(MESSAGE_DELETE, userId), cause);
  }

  public static SearchIndexException becauseSearchFailed(final Throwable cause) {
    return new SearchIndexException(MESSAGE_SEARCH, cause);
  }

  public static SearchIndexException becauseFindFailed(
      final String userId, final Throwable cause) {
    return new SearchIndexException(String.format(MESSAGE_FIND, userId), cause);
  }
}
