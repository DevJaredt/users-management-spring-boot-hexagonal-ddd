package com.jcaa.accounts.application.service;

import com.jcaa.accounts.application.port.in.CreateAccountUseCase;
import com.jcaa.accounts.application.port.out.SaveAccountPort;
import com.jcaa.accounts.application.service.dto.command.CreateAccountCommand;
import com.jcaa.accounts.domain.model.AccountModel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Alta de cuenta (paso 6 del diagrama). */
@Service
@RequiredArgsConstructor
public class CreateAccountService implements CreateAccountUseCase {

  private final SaveAccountPort saveAccountPort;
  private final Validator validator;

  @Override
  public AccountModel execute(final CreateAccountCommand command) {
    validateCommand(command);

    final AccountModel account =
        new AccountModel(
            UUID.randomUUID().toString(), command.userId(), command.name(), command.email());
    return saveAccountPort.save(account);
  }

  private void validateCommand(final CreateAccountCommand command) {
    final Set<ConstraintViolation<CreateAccountCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
