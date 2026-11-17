package com.jcaa.search.application.port.in;

import com.jcaa.search.application.service.dto.command.IndexUserCommand;

/** Caso de uso: mantener sincronizado el índice con los eventos de usuario (lado de escritura). */
public interface SyncUserIndexUseCase {

  void index(IndexUserCommand command);

  void delete(String userId);
}
