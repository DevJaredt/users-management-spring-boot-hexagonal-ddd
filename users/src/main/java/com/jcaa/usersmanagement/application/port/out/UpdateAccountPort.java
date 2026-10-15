package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.application.port.out.dto.AccountRequest;

/**
 * Puerto de salida para actualizar la cuenta asociada a un usuario (sincrono).
 *
 * <p>Es el espejo del alta ({@link CreateAccountPort}): al actualizar un usuario se sincronizan el
 * nombre y el correo en el servicio accounts.
 */
public interface UpdateAccountPort {

  void updateAccount(AccountRequest request);
}
