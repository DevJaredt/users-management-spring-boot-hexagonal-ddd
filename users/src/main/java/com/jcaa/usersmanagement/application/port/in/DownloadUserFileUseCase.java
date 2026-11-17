package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.port.out.dto.StoredFile;
import com.jcaa.usersmanagement.application.service.dto.query.DownloadUserFileQuery;

/** Caso de uso: descargar una foto o PDF asociado a un usuario. */
public interface DownloadUserFileUseCase {

  StoredFile execute(DownloadUserFileQuery query);
}
