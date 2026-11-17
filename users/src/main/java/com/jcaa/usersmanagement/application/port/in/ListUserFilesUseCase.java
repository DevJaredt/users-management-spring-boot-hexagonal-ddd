package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.application.service.dto.query.ListUserFilesQuery;
import java.util.List;

/** Caso de uso: listar los archivos de un usuario. */
public interface ListUserFilesUseCase {

  List<StoredFileMetadata> execute(ListUserFilesQuery query);
}
