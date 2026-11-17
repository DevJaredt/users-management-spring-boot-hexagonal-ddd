package com.jcaa.usersmanagement.infrastructure.adapter.storage;

import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFile;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.domain.enums.UserFileContentType;
import com.jcaa.usersmanagement.domain.exception.UserFileStorageUnavailableException;
import com.jcaa.usersmanagement.domain.valueobject.UserFileName;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Implementación usada cuando Backblaze B2 no está configurado.
 *
 * <p>Permite arrancar el servicio en local sin credenciales; los endpoints de archivos responden 503
 * con un mensaje claro hasta que se configure el bucket.
 */
public class UnavailableUserFileStorageAdapter implements UserFileStoragePort {

  @Override
  public StoredFileMetadata store(
      final UserId ownerId,
      final UserFileName fileName,
      final byte[] content,
      final UserFileContentType contentType) {
    throw UserFileStorageUnavailableException.becauseItIsNotConfigured();
  }

  @Override
  public Optional<StoredFile> load(final UserId ownerId, final UserFileName fileName) {
    throw UserFileStorageUnavailableException.becauseItIsNotConfigured();
  }

  @Override
  public List<StoredFileMetadata> list(final UserId ownerId) {
    throw UserFileStorageUnavailableException.becauseItIsNotConfigured();
  }

  @Override
  public void delete(final UserId ownerId, final UserFileName fileName) {
    throw UserFileStorageUnavailableException.becauseItIsNotConfigured();
  }

  @Override
  public boolean isAvailable() {
    return false;
  }
}
