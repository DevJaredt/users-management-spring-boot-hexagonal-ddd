package com.jcaa.accounts.application.port.out;

import com.jcaa.accounts.domain.model.AccountModel;

public interface SaveAccountPort {

  /** Debe ser idempotente: si el usuario ya tiene cuenta, devuelve la existente. */
  AccountModel save(AccountModel account);
}
