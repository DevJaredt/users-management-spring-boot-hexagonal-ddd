package com.jcaa.usersmanagement.domain.enums;

/**
 * Tipos de archivo permitidos para un usuario: fotografías o documentos PDF.
 *
 * <p>La Guía 9 almacena archivos privados en Backblaze B2; el dominio restringe qué se acepta antes
 * de enviarlo al bucket.
 */
public enum UserFileContentType {
  IMAGE_JPEG("image/jpeg", "jpg", "jpeg"),
  IMAGE_PNG("image/png", "png"),
  IMAGE_WEBP("image/webp", "webp"),
  IMAGE_GIF("image/gif", "gif"),
  APPLICATION_PDF("application/pdf", "pdf");

  private final String mimeType;
  private final String[] extensions;

  UserFileContentType(final String mimeType, final String... extensions) {
    this.mimeType = mimeType;
    this.extensions = extensions.clone();
  }

  public String mimeType() {
    return mimeType;
  }

  public boolean supportsExtension(final String extension) {
    for (final String supported : extensions) {
      if (supported.equalsIgnoreCase(extension)) {
        return true;
      }
    }
    return false;
  }
}
