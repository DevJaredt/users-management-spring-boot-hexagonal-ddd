package com.jcaa.usersmanagement.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.infrastructure.adapter.cache.NoOpUserCacheAdapter;
import com.jcaa.usersmanagement.infrastructure.adapter.cache.ValkeyUserCacheAdapter;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

/**
 * Cablea la caché distribuida (Guía 10).
 *
 * <p>Con {@code CACHE_ENABLED=true} se crea un pool Jedis contra la URI de Aiven
 * ({@code rediss://...}); en caso contrario se registra una implementación no-op para que el
 * servicio funcione en local.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class ValkeySpringConfig {

  private static final String TLS_SCHEME = "rediss";
  private static final String USER_INFO_SEPARATOR = ":";
  private static final String DEFAULT_USER = "default";
  private static final String LOG_INIT = "[ValkeySpringConfig] cache Valkey habilitada. host={} port={} tls={}";
  private static final String LOG_DISABLED = "[ValkeySpringConfig] cache deshabilitada (CACHE_ENABLED=false)";

  @Bean
  @ConditionalOnProperty(name = "cache.enabled", havingValue = "false", matchIfMissing = true)
  public UserCachePort noOpUserCachePort() {
    log.info(LOG_DISABLED);
    return new NoOpUserCacheAdapter();
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
  public JedisPooled jedisPooled(
      @Value("${cache.uri}") final String cacheUri,
      @Value("${cache.connect-timeout}") final java.time.Duration connectTimeout) {
    final URI uri = URI.create(cacheUri);
    final boolean tls = TLS_SCHEME.equalsIgnoreCase(uri.getScheme());
    final HostAndPort hostAndPort = new HostAndPort(uri.getHost(), uri.getPort());
    final DefaultJedisClientConfig clientConfig =
        DefaultJedisClientConfig.builder()
            .ssl(tls)
            .user(resolveUser(uri))
            .password(resolvePassword(uri))
            .connectionTimeoutMillis((int) connectTimeout.toMillis())
            .build();

    log.info(LOG_INIT, hostAndPort.getHost(), hostAndPort.getPort(), tls);
    return new JedisPooled(hostAndPort, clientConfig);
  }

  @Bean
  @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
  public UserCachePort valkeyUserCachePort(
      final JedisPooled jedisPooled,
      final ObjectMapper objectMapper,
      @Value("${cache.key-prefix}") final String keyPrefix,
      @Value("${cache.ttl-seconds}") final long ttlSeconds) {
    return new ValkeyUserCacheAdapter(jedisPooled, objectMapper, keyPrefix, ttlSeconds);
  }

  private String resolveUser(final URI uri) {
    final String userInfo = uri.getUserInfo();
    if (userInfo == null || userInfo.isBlank()) {
      return DEFAULT_USER;
    }
    final int separatorIndex = userInfo.indexOf(USER_INFO_SEPARATOR);
    return separatorIndex < 0 ? userInfo : userInfo.substring(0, separatorIndex);
  }

  private String resolvePassword(final URI uri) {
    final String userInfo = uri.getUserInfo();
    if (userInfo == null || userInfo.isBlank()) {
      return null;
    }
    final int separatorIndex = userInfo.indexOf(USER_INFO_SEPARATOR);
    if (separatorIndex < 0 || separatorIndex == userInfo.length() - 1) {
      return null;
    }
    return URLDecoder.decode(userInfo.substring(separatorIndex + 1), StandardCharsets.UTF_8);
  }
}
