package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.out.CreateAccountPort;
import com.jcaa.usersmanagement.application.port.out.GetUserByEmailPort;
import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import com.jcaa.usersmanagement.application.port.out.UserEventPublisherPort;
import com.jcaa.usersmanagement.application.port.out.dto.AccountRequest;
import com.jcaa.usersmanagement.application.port.out.dto.UserEvent;
import com.jcaa.usersmanagement.application.port.out.dto.UserEventType;
import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.mapper.UserApplicationMapper;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import jakarta.validation.ConstraintViolation;

/**
 * Alta de usuario: persiste, crea la cuenta asociada (paso 5, sincrono) y publica el evento de
 * dominio en Kafka (paso 2, asincrono).
 */
@Service
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

  private final SaveUserPort saveUserPort;
  private final GetUserByEmailPort getUserByEmailPort;
  private final CreateAccountPort createAccountPort;
  private final UserEventPublisherPort userEventPublisherPort;
  private final Validator validator;

  @Override
  public UserModel execute(final CreateUserCommand command) {
    validateCommand(command);

    final UserEmail email = new UserEmail(command.email());
    ensureEmailIsNotTaken(email);

    final UserModel userToSave = UserApplicationMapper.fromCreateCommandToModel(command);
    final UserModel savedUser = saveUserPort.save(userToSave);

    createAccount(savedUser);
    publishUserEvent(savedUser, UserEventType.USER_CREATED);

    return savedUser;
  }

  private void createAccount(final UserModel user) {
    createAccountPort.createAccount(
        new AccountRequest(user.getId().value(), user.getName().value(), user.getEmail().value()));
  }

  private void publishUserEvent(final UserModel user, final UserEventType eventType) {
    userEventPublisherPort.publish(
        new UserEvent(
            eventType,
            user.getId().value(),
            user.getName().value(),
            user.getEmail().value(),
            user.getRole().name(),
            user.getStatus().name()));
  }

  private void validateCommand(final CreateUserCommand command) {
    final Set<ConstraintViolation<CreateUserCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private void ensureEmailIsNotTaken(final UserEmail email) {
    getUserByEmailPort
        .getByEmail(email)
        .ifPresent(
            ignored -> {
              throw UserAlreadyExistsException.becauseEmailAlreadyExists(email.value());
            });
  }
}
