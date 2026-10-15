package com.jcaa.accounts.application.service;

import com.jcaa.accounts.application.port.in.CreateAccountUseCase;
import com.jcaa.accounts.application.port.out.AccountEventPublisherPort;
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

/**
 * Alta de cuenta (paso 6 del diagrama). Tras persistir, publica el evento {@code create-account}
 * (paso 7, asincrono) para que el servicio notifiers envie el correo.
 */
@Service
@RequiredArgsConstructor
public class CreateAccountService implements CreateAccountUseCase {

  private final SaveAccountPort saveAccountPort;
  private final AccountEventPublisherPort accountEventPublisherPort;
  private final Validator validator;

  @Override
  public AccountModel execute(final CreateAccountCommand command) {
    validateCommand(command);

    final AccountModel account =
        new AccountModel(
            UUID.randomUUID().toString(), command.userId(), command.name(), command.email());
    final AccountModel saved = saveAccountPort.save(account);

    accountEventPublisherPort.publish(saved);

    return saved;
  }

  private void validateCommand(final CreateAccountCommand command) {
    final Set<ConstraintViolation<CreateAccountCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
