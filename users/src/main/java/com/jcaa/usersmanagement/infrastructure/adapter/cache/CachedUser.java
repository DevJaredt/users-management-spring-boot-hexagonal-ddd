package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;

/**
 * Representación serializable de un usuario en la caché distribuida.
 *
 * <p>Se guarda el hash BCrypt (nunca la contraseña en texto plano) para poder reconstruir el
 * {@link UserModel} sin volver a MySQL. La caché está protegida por TLS y credenciales de Aiven.
 */
public record CachedUser(
    String id,
    String name,
    String email,
    String passwordHash,
    String role,
    String status) {

  public static CachedUser from(final UserModel user) {
    return new CachedUser(
        user.getId().value(),
        user.getName().value(),
        user.getEmail().value(),
        user.getPassword().value(),
        user.getRole().name(),
        user.getStatus().name());
  }

  public UserModel toModel() {
    return new UserModel(
        new UserId(id),
        new UserName(name),
        new UserEmail(email),
        UserPassword.fromHash(passwordHash),
        UserRole.valueOf(role),
        UserStatus.valueOf(status));
  }
}
