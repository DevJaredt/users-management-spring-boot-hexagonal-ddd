package com.jcaa.accounts.infrastructure.entrypoint.rest.advice;

import com.jcaa.accounts.domain.exception.AccountNotFoundException;
import com.jcaa.accounts.infrastructure.adapter.persistence.exception.AccountPersistenceException;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String VALIDATION_ERROR = "Datos de la solicitud invalidos.";
  private static final String PERSISTENCE_ERROR = "Error de persistencia.";

  @ExceptionHandler(AccountNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleNotFound(final AccountNotFoundException exception) {
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
  }

  @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleValidation(final Exception exception) {
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), VALIDATION_ERROR);
  }

  @ExceptionHandler(AccountPersistenceException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handlePersistence(final AccountPersistenceException exception) {
    return new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), PERSISTENCE_ERROR);
  }
}
