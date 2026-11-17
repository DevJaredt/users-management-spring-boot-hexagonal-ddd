package com.jcaa.usersmanagement.infrastructure.adapter.storage.exception;

/** Falla al comunicarse con el proveedor de almacenamiento de objetos (Backblaze B2). */
public final class FileStorageException extends RuntimeException {

  private static final String MESSAGE_UPLOAD = "No se pudo subir el archivo '%s'.";
  private static final String MESSAGE_DOWNLOAD = "No se pudo descargar el archivo '%s'.";
  private static final String MESSAGE_LIST = "No se pudieron listar los archivos del usuario.";
  private static final String MESSAGE_DELETE = "No se pudo eliminar el archivo '%s'.";

  private FileStorageException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static FileStorageException becauseUploadFailed(
      final String fileName, final Throwable cause) {
    return new FileStorageException(String.format(MESSAGE_UPLOAD, fileName), cause);
  }

  public static FileStorageException becauseDownloadFailed(
      final String fileName, final Throwable cause) {
    return new FileStorageException(String.format(MESSAGE_DOWNLOAD, fileName), cause);
  }

  public static FileStorageException becauseListFailed(final Throwable cause) {
    return new FileStorageException(MESSAGE_LIST, cause);
  }

  public static FileStorageException becauseDeleteFailed(
      final String fileName, final Throwable cause) {
    return new FileStorageException(String.format(MESSAGE_DELETE, fileName), cause);
  }
}
