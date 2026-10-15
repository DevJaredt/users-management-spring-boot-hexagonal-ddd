package com.jcaa.usersmanagement.infrastructure.adapter.accounts;

import com.jcaa.usersmanagement.application.port.out.CreateAccountPort;
import com.jcaa.usersmanagement.application.port.out.UpdateAccountPort;
import com.jcaa.usersmanagement.application.port.out.dto.AccountRequest;
import com.jcaa.usersmanagement.infrastructure.adapter.accounts.dto.AccountCreationRequest;
import com.jcaa.usersmanagement.infrastructure.adapter.accounts.dto.AccountUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador del puerto de cuentas: llama por HTTP al microservicio <b>accounts</b>
 * (paso 5 del diagrama, sincrono).
 *
 * <p>Si accounts no responde se registra un aviso y la operacion del usuario no se revierte: la
 * consistencia entre ambos servicios es eventual. En los logs solo van identificadores tecnicos.
 */
@Slf4j
@Component
public class AccountsRestClientAdapter implements CreateAccountPort, UpdateAccountPort {

  private static final String ACCOUNTS_PATH = "/api/accounts";
  private static final String LOG_CREATED = "[AccountsRestClientAdapter] cuenta creada. userId={}";
  private static final String LOG_CREATE_FAILED =
      "[AccountsRestClientAdapter] no se pudo crear la cuenta. userId={} causa={}";
  private static final String LOG_UPDATED =
      "[AccountsRestClientAdapter] cuenta actualizada. userId={}";
  private static final String LOG_UPDATE_FAILED =
      "[AccountsRestClientAdapter] no se pudo actualizar la cuenta. userId={} causa={}";

  private final RestClient accountsRestClient;

  public AccountsRestClientAdapter(final RestClient accountsRestClient) {
    this.accountsRestClient = accountsRestClient;
  }

  @Override
  public void createAccount(final AccountRequest request) {
    try {
      accountsRestClient
          .post()
          .uri(ACCOUNTS_PATH)
          .body(new AccountCreationRequest(request.userId(), request.name(), request.email()))
          .retrieve()
          .toBodilessEntity();
      log.info(LOG_CREATED, request.userId());
    } catch (final RestClientException exception) {
      log.warn(LOG_CREATE_FAILED, request.userId(), exception.getClass().getSimpleName());
    }
  }

  @Override
  public void updateAccount(final AccountRequest request) {
    try {
      accountsRestClient
          .put()
          .uri(ACCOUNTS_PATH + "/" + request.userId())
          .body(new AccountUpdateRequest(request.name(), request.email()))
          .retrieve()
          .toBodilessEntity();
      log.info(LOG_UPDATED, request.userId());
    } catch (final RestClientException exception) {
      log.warn(LOG_UPDATE_FAILED, request.userId(), exception.getClass().getSimpleName());
    }
  }
}
