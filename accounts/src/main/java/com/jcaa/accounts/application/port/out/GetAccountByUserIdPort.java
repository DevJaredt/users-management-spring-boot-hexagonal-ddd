package com.jcaa.accounts.application.port.out;

import com.jcaa.accounts.domain.model.AccountModel;
import java.util.Optional;

public interface GetAccountByUserIdPort {

  Optional<AccountModel> getByUserId(String userId);
}
