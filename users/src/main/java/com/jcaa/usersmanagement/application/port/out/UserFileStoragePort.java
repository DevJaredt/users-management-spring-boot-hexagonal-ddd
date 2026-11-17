package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.application.port.out.dto.StoredFile;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.domain.enums.UserFileContentType;
import com.jcaa.usersmanagement.domain.valueobject.UserFileName;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para almacenar archivos de usuario.
 *
 * <p>La implementación concreta es Backblaze B2 (compatible con S3), pero la aplicación no conoce
 * el proveedor. La implementación debe aislar los objetos por usuario bajo el prefijo
 * {@code users/{userId}/}.
 */
public interface UserFileStoragePort {

  StoredFileMetadata store(
      UserId ownerId, UserFileName fileName, byte[] content, UserFileContentType contentType);

  Optional<StoredFile> load(UserId ownerId, UserFileName fileName);

  List<StoredFileMetadata> list(UserId ownerId);

  void delete(UserId ownerId, UserFileName fileName);

  /** Indica si el almacenamiento está configurado (credenciales y bucket presentes). */
  boolean isAvailable();
}
