package com.jcaa.accounts.infrastructure.entrypoint.rest.mapper;

import com.jcaa.accounts.application.service.dto.command.CreateAccountCommand;
import com.jcaa.accounts.application.service.dto.command.UpdateAccountCommand;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.request.CreateAccountRestRequest;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.request.UpdateAccountRestRequest;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.response.AccountRestResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountRestMapper {

  public CreateAccountCommand toCreateCommand(final CreateAccountRestRequest request) {
    return new CreateAccountCommand(request.userId(), request.name(), request.email());
  }

  public UpdateAccountCommand toUpdateCommand(
      final String userId, final UpdateAccountRestRequest request) {
    return new UpdateAccountCommand(userId, request.name(), request.email());
  }

  public AccountRestResponse toResponse(final AccountModel account) {
    return new AccountRestResponse(
        account.id(), account.userId(), account.name(), account.email());
  }
}
