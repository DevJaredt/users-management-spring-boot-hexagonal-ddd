package com.jcaa.usersmanagement.domain.valueobject;

import com.jcaa.usersmanagement.domain.exception.InvalidUserFileException;
import java.util.Objects;

/**
 * Nombre de un archivo asociado a un usuario.
 *
 * <p>Se normaliza para que solo contenga caracteres seguros (sin separadores de ruta) y así evitar
 * que un nombre malicioso escape de la carpeta del usuario dentro del bucket.
 */
public record UserFileName(String value) {

  private static final int MAXIMUM_LENGTH = 120;
  private static final String REPLACEMENT = "_";
  private static final String UNSAFE_CHARACTERS = "[^A-Za-z0-9._-]";
  private static final String PATH_SEPARATOR_PATTERN = "[/\\\\]";

  public UserFileName {
    final String withoutPath = removePath(Objects.requireNonNull(value, "FileName cannot be null"));
    final String normalizedValue = sanitize(withoutPath);
    validateNotEmpty(normalizedValue);
    validateLength(normalizedValue);
    value = normalizedValue;
  }

  public static UserFileName from(final String fileName) {
    return new UserFileName(fileName);
  }

  private static String removePath(final String rawValue) {
    final String[] segments = rawValue.trim().split(PATH_SEPARATOR_PATTERN);
    return segments.length == 0 ? rawValue : segments[segments.length - 1];
  }

  private static String sanitize(final String rawValue) {
    return rawValue.trim().replaceAll(UNSAFE_CHARACTERS, REPLACEMENT);
  }

  private static void validateNotEmpty(final String normalizedValue) {
    if (normalizedValue.isEmpty() || normalizedValue.equals(".") || normalizedValue.equals("..")) {
      throw InvalidUserFileException.becauseFileNameIsInvalid();
    }
  }

  private static void validateLength(final String normalizedValue) {
    if (normalizedValue.length() > MAXIMUM_LENGTH) {
      throw InvalidUserFileException.becauseFileNameIsTooLong(MAXIMUM_LENGTH);
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
