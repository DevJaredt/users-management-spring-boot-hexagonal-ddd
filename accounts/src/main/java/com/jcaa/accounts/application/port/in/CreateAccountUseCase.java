package com.jcaa.accounts.application.port.in;

import com.jcaa.accounts.application.service.dto.command.CreateAccountCommand;
import com.jcaa.accounts.domain.model.AccountModel;

public interface CreateAccountUseCase {

  AccountModel execute(CreateAccountCommand command);
}
