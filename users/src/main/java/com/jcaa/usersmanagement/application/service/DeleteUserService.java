package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.DeleteUserUseCase;
import com.jcaa.usersmanagement.application.port.out.DeleteUserPort;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.application.port.out.UserEventPublisherPort;
import com.jcaa.usersmanagement.application.port.out.dto.UserEvent;
import com.jcaa.usersmanagement.application.port.out.dto.UserEventType;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserCommand;
import com.jcaa.usersmanagement.application.service.mapper.UserApplicationMapper;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Baja de usuario: elimina el registro, publica el evento {@code delete-user} para que el índice de
 * búsqueda retire el documento y limpia la entrada de caché.
 */
@Service
@RequiredArgsConstructor
public class DeleteUserService implements DeleteUserUseCase {

  private final DeleteUserPort deleteUserPort;
  private final GetUserByIdPort getUserByIdPort;
  private final UserEventPublisherPort userEventPublisherPort;
  private final UserCachePort userCachePort;
  private final Validator validator;

  @Override
  public void execute(final DeleteUserCommand command) {
    validateCommand(command);

    final UserId userId = UserApplicationMapper.fromDeleteCommandToUserId(command);
    final UserModel existingUser = findUserOrFail(userId);

    deleteUserPort.delete(userId);

    publishUserEvent(existingUser);
    userCachePort.evict(userId);
  }

  private void publishUserEvent(final UserModel user) {
    userEventPublisherPort.publish(
        new UserEvent(
            UserEventType.USER_DELETED,
            user.getId().value(),
            user.getName().value(),
            user.getEmail().value(),
            user.getRole().name(),
            user.getStatus().name()));
  }

  private void validateCommand(final DeleteUserCommand command) {
    final Set<ConstraintViolation<DeleteUserCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private UserModel findUserOrFail(final UserId userId) {
    return getUserByIdPort
        .getById(userId)
        .orElseThrow(() -> UserNotFoundException.becauseIdWasNotFound(userId.value()));
  }
}
