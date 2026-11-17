package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;

/**
 * Adaptador de caché distribuida sobre Valkey/Redis en Aiven (Guía 10).
 *
 * <p>La caché es <b>fail-open</b>: si el proveedor falla, se registra un aviso técnico y la
 * aplicación continúa leyendo de MySQL. Se usa {@code SETEX} para que cada entrada expire según el
 * TTL configurado.
 */
@Slf4j
@RequiredArgsConstructor
public class ValkeyUserCacheAdapter implements UserCachePort {

  private static final String LOG_HIT = "[ValkeyUserCacheAdapter] cache hit. userId={}";
  private static final String LOG_STORED = "[ValkeyUserCacheAdapter] usuario cacheado. userId={} ttl={}s";
  private static final String LOG_EVICTED = "[ValkeyUserCacheAdapter] entrada de cache eliminada. userId={}";
  private static final String LOG_READ_FAILED =
      "[ValkeyUserCacheAdapter] no se pudo leer la cache. userId={} causa={}";
  private static final String LOG_WRITE_FAILED =
      "[ValkeyUserCacheAdapter] no se pudo escribir en la cache. userId={} causa={}";
  private static final String LOG_EVICT_FAILED =
      "[ValkeyUserCacheAdapter] no se pudo eliminar la entrada de cache. userId={} causa={}";

  private final JedisPooled jedisPooled;
  private final ObjectMapper objectMapper;
  private final String keyPrefix;
  private final long ttlSeconds;

  @Override
  public Optional<UserModel> getById(final UserId userId) {
    try {
      final String payload = jedisPooled.get(buildKey(userId));
      if (payload == null || payload.isBlank()) {
        return Optional.empty();
      }
      log.info(LOG_HIT, userId.value());
      return Optional.of(objectMapper.readValue(payload, CachedUser.class).toModel());
    } catch (final JsonProcessingException | RuntimeException exception) {
      log.warn(LOG_READ_FAILED, userId.value(), exception.getClass().getSimpleName());
      return Optional.empty();
    }
  }

  @Override
  public void put(final UserModel user) {
    try {
      final String payload = objectMapper.writeValueAsString(CachedUser.from(user));
      jedisPooled.setex(buildKey(user.getId()), ttlSeconds, payload);
      log.info(LOG_STORED, user.getId().value(), ttlSeconds);
    } catch (final JsonProcessingException | RuntimeException exception) {
      log.warn(LOG_WRITE_FAILED, user.getId().value(), exception.getClass().getSimpleName());
    }
  }

  @Override
  public void evict(final UserId userId) {
    try {
      jedisPooled.del(buildKey(userId));
      log.info(LOG_EVICTED, userId.value());
    } catch (final RuntimeException exception) {
      log.warn(LOG_EVICT_FAILED, userId.value(), exception.getClass().getSimpleName());
    }
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  private String buildKey(final UserId userId) {
    return keyPrefix + userId.value();
  }
}
