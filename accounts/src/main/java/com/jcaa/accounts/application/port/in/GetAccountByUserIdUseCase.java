package com.jcaa.accounts.application.port.in;

import com.jcaa.accounts.domain.model.AccountModel;

public interface GetAccountByUserIdUseCase {

  AccountModel execute(String userId);
}
