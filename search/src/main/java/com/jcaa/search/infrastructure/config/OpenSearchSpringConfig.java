package com.jcaa.search.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.search.application.port.out.UserSearchIndexPort;
import com.jcaa.search.infrastructure.adapter.opensearch.NoOpUserSearchIndexAdapter;
import com.jcaa.search.infrastructure.adapter.opensearch.OpenSearchUserIndexAdapter;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cablea el cliente REST de OpenSearch (Guía 11).
 *
 * <p>Con {@code OPENSEARCH_ENABLED=true} se crea un {@link RestClient} con autenticación básica y se
 * inicializa el índice {@code users}. Sin credenciales se usa una implementación no-op para que el
 * servicio arranque en local.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class OpenSearchSpringConfig {

  private static final String LOG_DISABLED =
      "[OpenSearchSpringConfig] OpenSearch deshabilitado (OPENSEARCH_ENABLED=false)";
  private static final String LOG_INIT =
      "[OpenSearchSpringConfig] OpenSearch habilitado. index={}";

  @Bean
  @ConditionalOnProperty(name = "opensearch.enabled", havingValue = "false", matchIfMissing = true)
  public UserSearchIndexPort noOpUserSearchIndexPort() {
    log.info(LOG_DISABLED);
    return new NoOpUserSearchIndexAdapter();
  }

  @Bean
  @ConditionalOnProperty(name = "opensearch.enabled", havingValue = "true")
  public RestClient openSearchRestClient(
      @Value("${opensearch.url}") final String url,
      @Value("${opensearch.username}") final String username,
      @Value("${opensearch.password}") final String password,
      @Value("${opensearch.connect-timeout}") final Duration connectTimeout,
      @Value("${opensearch.read-timeout}") final Duration readTimeout) {
    if (url == null || url.isBlank()) {
      throw new IllegalStateException("OPENSEARCH_ENABLED=true requiere OPENSEARCH_URL.");
    }

    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);

    return RestClient.builder()
        .baseUrl(url)
        .requestFactory(requestFactory)
        .defaultHeaders(headers -> headers.setBasicAuth(username, password))
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "opensearch.enabled", havingValue = "true")
  public UserSearchIndexPort openSearchUserIndexPort(
      final RestClient openSearchRestClient,
      final ObjectMapper objectMapper,
      @Value("${opensearch.index}") final String indexName) {
    log.info(LOG_INIT, indexName);
    return new OpenSearchUserIndexAdapter(openSearchRestClient, objectMapper, indexName);
  }

  @Bean
  @ConditionalOnProperty(name = "opensearch.enabled", havingValue = "true")
  public ApplicationRunner openSearchIndexInitializer(final UserSearchIndexPort indexPort) {
    return args -> {
      if (indexPort instanceof OpenSearchUserIndexAdapter adapter) {
        adapter.ensureIndex();
      }
    };
  }
}
