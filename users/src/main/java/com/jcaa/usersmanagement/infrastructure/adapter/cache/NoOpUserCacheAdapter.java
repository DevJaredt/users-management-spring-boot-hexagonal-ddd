package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.Optional;

/**
 * Implementación usada cuando la caché está deshabilitada (desarrollo local sin Aiven).
 *
 * <p>Mantiene la aplicación arrancable sin credenciales cloud y sin cambiar los casos de uso.
 */
public class NoOpUserCacheAdapter implements UserCachePort {

  @Override
  public Optional<UserModel> getById(final UserId userId) {
    return Optional.empty();
  }

  @Override
  public void put(final UserModel user) {
    // Sin caché configurada no hay nada que almacenar.
  }

  @Override
  public void evict(final UserId userId) {
    // Sin caché configurada no hay nada que invalidar.
  }

  @Override
  public boolean isEnabled() {
    return false;
  }
}
