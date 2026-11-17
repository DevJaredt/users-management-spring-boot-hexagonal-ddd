package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.Optional;

/**
 * Puerto de salida para la caché distribuida (Valkey/Redis).
 *
 * <p>La caché es un acelerador opcional: si el proveedor no está disponible, la implementación debe
 * degradar y permitir que la aplicación siga leyendo de la base de datos.
 */
public interface UserCachePort {

  Optional<UserModel> getById(UserId userId);

  void put(UserModel user);

  void evict(UserId userId);

  boolean isEnabled();
}
