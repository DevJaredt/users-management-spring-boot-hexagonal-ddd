package com.jcaa.accounts.application.port.in;

import com.jcaa.accounts.application.service.dto.command.UpdateAccountCommand;
import com.jcaa.accounts.domain.model.AccountModel;

public interface UpdateAccountUseCase {

  AccountModel execute(UpdateAccountCommand command);
}
