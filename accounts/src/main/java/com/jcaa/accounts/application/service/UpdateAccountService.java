package com.jcaa.accounts.application.service;

import com.jcaa.accounts.application.port.in.UpdateAccountUseCase;
import com.jcaa.accounts.application.port.out.UpdateAccountPort;
import com.jcaa.accounts.application.service.dto.command.UpdateAccountCommand;
import com.jcaa.accounts.domain.model.AccountModel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Actualizacion de la cuenta asociada a un usuario.
 *
 * <p>La invoca el servicio <b>users</b> de forma sincrona cuando actualiza un usuario, para
 * mantener sincronizados el nombre y el correo entre {@code users} y {@code accounts}.
 */
@Service
@RequiredArgsConstructor
public class UpdateAccountService implements UpdateAccountUseCase {

  private final UpdateAccountPort updateAccountPort;
  private final Validator validator;

  @Override
  public AccountModel execute(final UpdateAccountCommand command) {
    validateCommand(command);
    return updateAccountPort.updateByUserId(command.userId(), command.name(), command.email());
  }

  private void validateCommand(final UpdateAccountCommand command) {
    final Set<ConstraintViolation<UpdateAccountCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
