package com.jcaa.search.infrastructure.entrypoint.rest.advice;

import com.jcaa.search.domain.exception.SearchUnavailableException;
import com.jcaa.search.domain.exception.UserNotIndexedException;
import com.jcaa.search.infrastructure.adapter.opensearch.exception.SearchIndexException;
import com.jcaa.search.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserNotIndexedException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleNotIndexed(final UserNotIndexedException exception) {
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
  }

  @ExceptionHandler(SearchUnavailableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ApiErrorResponse handleUnavailable(final SearchUnavailableException exception) {
    return new ApiErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), exception.getMessage());
  }

  @ExceptionHandler(SearchIndexException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public ApiErrorResponse handleIndexFailure(final SearchIndexException ignored) {
    return new ApiErrorResponse(
        HttpStatus.BAD_GATEWAY.value(), "Error al comunicarse con el índice de búsqueda.");
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleConstraintViolation(final ConstraintViolationException exception) {
    final String message =
        exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleIllegalArgument(final IllegalArgumentException exception) {
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handleGeneral(final Exception ignored) {
    return new ApiErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor.");
  }
}
