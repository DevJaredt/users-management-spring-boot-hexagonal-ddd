package com.jcaa.accounts.infrastructure.entrypoint.rest.controller;

import com.jcaa.accounts.application.port.in.CreateAccountUseCase;
import com.jcaa.accounts.application.port.in.GetAccountByUserIdUseCase;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.request.CreateAccountRestRequest;
import com.jcaa.accounts.infrastructure.entrypoint.rest.dto.response.AccountRestResponse;
import com.jcaa.accounts.infrastructure.entrypoint.rest.mapper.AccountRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del servicio accounts (paso 6 del diagrama).
 *
 * <p>{@code POST /api/accounts} es el endpoint que invoca el servicio users de forma sincrona.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

  private final CreateAccountUseCase createAccountUseCase;
  private final GetAccountByUserIdUseCase getAccountByUserIdUseCase;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountRestResponse create(@Valid @RequestBody final CreateAccountRestRequest request) {
    return AccountRestMapper.toResponse(
        createAccountUseCase.execute(AccountRestMapper.toCreateCommand(request)));
  }

  @GetMapping("/{userId}")
  public AccountRestResponse getByUserId(@PathVariable final String userId) {
    return AccountRestMapper.toResponse(getAccountByUserIdUseCase.execute(userId));
  }
}
