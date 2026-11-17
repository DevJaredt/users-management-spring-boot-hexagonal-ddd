package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserFileCommand;

/** Caso de uso: eliminar una foto o PDF asociado a un usuario. */
public interface DeleteUserFileUseCase {

  void execute(DeleteUserFileCommand command);
}
