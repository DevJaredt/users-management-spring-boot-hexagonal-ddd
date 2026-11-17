package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;

/** Caso de uso: subir una foto o PDF asociado a un usuario. */
public interface UploadUserFileUseCase {

  StoredFileMetadata execute(UploadUserFileCommand command);
}
