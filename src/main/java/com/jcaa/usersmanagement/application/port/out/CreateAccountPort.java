package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.application.port.out.dto.AccountRequest;

/**
 * Puerto de salida para crear la cuenta asociada a un usuario (paso 5 del diagrama: sincrono).
 */
public interface CreateAccountPort {

  void createAccount(AccountRequest request);
}
