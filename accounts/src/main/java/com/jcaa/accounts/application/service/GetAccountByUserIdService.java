package com.jcaa.accounts.application.service;

import com.jcaa.accounts.application.port.in.GetAccountByUserIdUseCase;
import com.jcaa.accounts.application.port.out.GetAccountByUserIdPort;
import com.jcaa.accounts.domain.exception.AccountNotFoundException;
import com.jcaa.accounts.domain.model.AccountModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAccountByUserIdService implements GetAccountByUserIdUseCase {

  private final GetAccountByUserIdPort getAccountByUserIdPort;

  @Override
  public AccountModel execute(final String userId) {
    return getAccountByUserIdPort
        .getByUserId(userId)
        .orElseThrow(() -> AccountNotFoundException.becauseUserIdWasNotFound(userId));
  }
}
