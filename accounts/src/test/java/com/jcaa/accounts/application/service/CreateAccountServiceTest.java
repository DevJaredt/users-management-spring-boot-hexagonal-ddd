package com.jcaa.accounts.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jcaa.accounts.application.port.out.SaveAccountPort;
import com.jcaa.accounts.application.service.dto.command.CreateAccountCommand;
import com.jcaa.accounts.domain.model.AccountModel;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("CreateAccountService")
@ExtendWith(MockitoExtension.class)
class CreateAccountServiceTest {

  private static final String USER_ID = "user-1";
  private static final String NAME = "Jared Trujillo";
  private static final String EMAIL = "jared@example.com";

  @Mock private SaveAccountPort saveAccountPort;

  private CreateAccountService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service = new CreateAccountService(saveAccountPort, validatorFactory.getValidator());
    }
  }

  @Test
  @DisplayName("execute() genera el id y persiste la cuenta")
  void shouldGenerateIdAndSave() {
    // Arrange
    when(saveAccountPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    final AccountModel result =
        service.execute(new CreateAccountCommand(USER_ID, NAME, EMAIL));

    // Assert
    assertAll(
        "cuenta creada",
        () -> assertNotNull(result.id(), "debe generar id"),
        () -> assertEquals(USER_ID, result.userId()),
        () -> assertEquals(NAME, result.name()),
        () -> assertEquals(EMAIL, result.email()));
    verify(saveAccountPort).save(any(AccountModel.class));
  }

  @Test
  @DisplayName("execute() rechaza un command invalido sin tocar la persistencia")
  void shouldRejectInvalidCommand() {
    // Arrange
    final CreateAccountCommand invalid = new CreateAccountCommand("  ", NAME, "no-es-un-email");

    // Act & Assert
    assertThrows(ConstraintViolationException.class, () -> service.execute(invalid));
    verifyNoInteractions(saveAccountPort);
  }
}
