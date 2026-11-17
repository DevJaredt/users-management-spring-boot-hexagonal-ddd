package com.jcaa.search.infrastructure.adapter.opensearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.search.application.port.out.UserSearchIndexPort;
import com.jcaa.search.domain.model.UserSearchDocument;
import com.jcaa.search.domain.model.UserSearchResult;
import com.jcaa.search.infrastructure.adapter.opensearch.exception.SearchIndexException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptador del índice de OpenSearch (Guía 11).
 *
 * <p>Aplica CQRS: los eventos de escritura indexan o eliminan documentos, y las consultas de
 * búsqueda se resuelven directamente contra el índice. Se usa la API REST con autenticación básica
 * sobre HTTPS (Aiven).
 */
@Slf4j
@RequiredArgsConstructor
public class OpenSearchUserIndexAdapter implements UserSearchIndexPort {

  private static final String PATH_DOC = "/{index}/_doc/{id}";
  private static final String PATH_SEARCH = "/{index}/_search";
  private static final String FIELD_SOURCE = "_source";
  private static final String FIELD_HITS = "hits";
  private static final String FIELD_TOTAL = "total";
  private static final String FIELD_VALUE = "value";
  private static final String FIELD_NAME_KEYWORD = "name.keyword";
  private static final int HTTP_BAD_REQUEST = 400;
  private static final int HTTP_NOT_FOUND = 404;
  private static final String LOG_INDEXED = "[OpenSearchUserIndexAdapter] usuario indexado. userId={}";
  private static final String LOG_DELETED = "[OpenSearchUserIndexAdapter] usuario eliminado del índice. userId={}";
  private static final String LOG_NOT_FOUND = "[OpenSearchUserIndexAdapter] documento no encontrado. userId={}";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String indexName;

  @Override
  public Optional<UserSearchDocument> findById(final String userId) {
    try {
      final String response =
          restClient
              .get()
              .uri(PATH_DOC, indexName, userId)
              .retrieve()
              .body(String.class);
      return parseSource(response);
    } catch (final HttpClientErrorException.NotFound exception) {
      log.info(LOG_NOT_FOUND, userId);
      return Optional.empty();
    } catch (final JsonProcessingException | RestClientResponseException
        | ResourceAccessException exception) {
      throw SearchIndexException.becauseFindFailed(userId, exception);
    }
  }

  @Override
  public UserSearchResult search(final String text, final int page, final int size) {
    try {
      final String body = objectMapper.writeValueAsString(buildSearchRequest(text, page, size));
      final String response =
          restClient
              .post()
              .uri(PATH_SEARCH, indexName)
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);
      return parseSearchResponse(response);
    } catch (final JsonProcessingException | RestClientResponseException
        | ResourceAccessException exception) {
      throw SearchIndexException.becauseSearchFailed(exception);
    }
  }

  @Override
  public void index(final UserSearchDocument document) {
    try {
      final String body = objectMapper.writeValueAsString(document);
      restClient
          .put()
          .uri(PATH_DOC, indexName, document.id())
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .toBodilessEntity();
      log.info(LOG_INDEXED, document.id());
    } catch (final JsonProcessingException | RestClientResponseException
        | ResourceAccessException exception) {
      throw SearchIndexException.becauseIndexFailed(document.id(), exception);
    }
  }

  @Override
  public void delete(final String userId) {
    try {
      restClient.delete().uri(PATH_DOC, indexName, userId).retrieve().toBodilessEntity();
      log.info(LOG_DELETED, userId);
    } catch (final HttpClientErrorException.NotFound exception) {
      log.info(LOG_NOT_FOUND, userId);
    } catch (final RestClientResponseException | ResourceAccessException exception) {
      throw SearchIndexException.becauseDeleteFailed(userId, exception);
    }
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  private Map<String, Object> buildSearchRequest(
      final String text, final int page, final int size) {
    final Map<String, Object> query = new LinkedHashMap<>();
    if (text == null || text.isBlank()) {
      query.put("match_all", Map.of());
    } else {
      final Map<String, Object> multiMatch = new LinkedHashMap<>();
      multiMatch.put("query", text);
      multiMatch.put("fields", List.of("name^3", "email^2", "role", "status"));
      multiMatch.put("fuzziness", "AUTO");
      query.put("multi_match", multiMatch);
    }

    final Map<String, Object> request = new LinkedHashMap<>();
    request.put("query", query);
    request.put("from", page * size);
    request.put("size", size);
    request.put("sort", List.of(Map.of("_score", Map.of("order", "desc")),
        Map.of(FIELD_NAME_KEYWORD, Map.of("order", "asc"))));
    return request;
  }

  private UserSearchResult parseSearchResponse(final String response) throws JsonProcessingException {
    final JsonNode root = objectMapper.readTree(response);
    final JsonNode hits = root.path(FIELD_HITS);
    final long total = hits.path(FIELD_TOTAL).path(FIELD_VALUE).asLong(0);
    final List<UserSearchDocument> users = new ArrayList<>();
    for (final JsonNode hit : hits.path("hits")) {
      parseDocument(hit.path(FIELD_SOURCE)).ifPresent(users::add);
    }
    return new UserSearchResult(users, total);
  }

  private Optional<UserSearchDocument> parseSource(final String response)
      throws JsonProcessingException {
    if (response == null || response.isBlank()) {
      return Optional.empty();
    }
    final JsonNode root = objectMapper.readTree(response);
    return parseDocument(root.path(FIELD_SOURCE));
  }

  private Optional<UserSearchDocument> parseDocument(final JsonNode source) {
    if (source.isMissingNode() || source.isNull() || source.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new UserSearchDocument(
            source.path("id").asText(),
            source.path("name").asText(),
            source.path("email").asText(),
            source.path("role").asText(),
            source.path("status").asText()));
  }

  /** Crea el índice con mappings explícitos; si ya existe, OpenSearch responde 400. */
  public void ensureIndex() {
    try {
      restClient
          .put()
          .uri("/{index}", indexName)
          .contentType(MediaType.APPLICATION_JSON)
          .body(indexDefinition())
          .retrieve()
          .toBodilessEntity();
      log.info("[OpenSearchUserIndexAdapter] índice creado. index={}", indexName);
    } catch (final HttpClientErrorException exception) {
      if (exception.getStatusCode().value() == HTTP_BAD_REQUEST) {
        log.info("[OpenSearchUserIndexAdapter] el índice ya existe. index={}", indexName);
        return;
      }
      log.warn(
          "[OpenSearchUserIndexAdapter] no se pudo crear el índice. index={} status={}",
          indexName,
          exception.getStatusCode().value());
    } catch (final ResourceAccessException exception) {
      log.warn(
          "[OpenSearchUserIndexAdapter] OpenSearch no responde al inicializar. index={} causa={}",
          indexName,
          exception.getClass().getSimpleName());
    }
  }

  private String indexDefinition() {
    return """
        {
          "settings": {"number_of_shards": 1, "number_of_replicas": 0},
          "mappings": {
            "properties": {
              "id": {"type": "keyword"},
              "name": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},
              "email": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},
              "role": {"type": "keyword"},
              "status": {"type": "keyword"}
            }
          }
        }
        """;
  }
}
