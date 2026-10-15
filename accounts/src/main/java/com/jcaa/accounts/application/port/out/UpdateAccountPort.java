package com.jcaa.accounts.application.port.out;

import com.jcaa.accounts.domain.model.AccountModel;

/** Puerto de salida: actualiza la cuenta asociada a un usuario. */
public interface UpdateAccountPort {

  AccountModel updateByUserId(String userId, String name, String email);
}
