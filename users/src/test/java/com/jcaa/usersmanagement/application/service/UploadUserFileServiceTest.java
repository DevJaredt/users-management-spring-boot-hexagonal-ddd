package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;
import com.jcaa.usersmanagement.domain.enums.UserFileContentType;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.InvalidUserFileException;
import com.jcaa.usersmanagement.domain.exception.UserFileStorageUnavailableException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserFileName;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests para UploadUserFileService (Guía 9: subida de archivos a Backblaze B2). */
@DisplayName("UploadUserFileService")
@ExtendWith(MockitoExtension.class)
class UploadUserFileServiceTest {

  private static final String USER_ID = "u-001";
  private static final long MAX_BYTES = 1_048_576L;
  private static final byte[] PDF_CONTENT = "%PDF-1.4 contenido".getBytes();

  @Mock private UserFileStoragePort userFileStoragePort;
  @Mock private GetUserByIdPort getUserByIdPort;

  private UploadUserFileService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service =
          new UploadUserFileService(
              userFileStoragePort,
              getUserByIdPort,
              new UserFileUploadPolicy(MAX_BYTES),
              validatorFactory.getValidator());
    }
  }

  @Test
  @DisplayName("execute() guarda un PDF válido en la carpeta del usuario")
  void shouldStoreFileWhenDataIsValid() {
    // Arrange
    final UploadUserFileCommand command =
        new UploadUserFileCommand(USER_ID, "certificado.pdf", "application/pdf", PDF_CONTENT);
    final StoredFileMetadata expected =
        new StoredFileMetadata("certificado.pdf", "application/pdf", PDF_CONTENT.length, Instant.now());

    when(getUserByIdPort.getById(new UserId(USER_ID))).thenReturn(Optional.of(existingUser()));
    when(userFileStoragePort.isAvailable()).thenReturn(true);
    when(userFileStoragePort.store(
            any(UserId.class),
            any(UserFileName.class),
            any(byte[].class),
            any(UserFileContentType.class)))
        .thenReturn(expected);

    // Act
    final StoredFileMetadata result = service.execute(command);

    // Assert
    assertAll(
        "subida válida",
        () -> assertNotNull(result),
        () -> assertEquals("certificado.pdf", result.fileName()),
        () -> assertEquals("application/pdf", result.contentType()));
    verify(userFileStoragePort)
        .store(
            new UserId(USER_ID),
            UserFileName.from("certificado.pdf"),
            PDF_CONTENT,
            UserFileContentType.APPLICATION_PDF);
  }

  @Test
  @DisplayName("execute() deduce el tipo desde la extensión si no viene el content type")
  void shouldInferContentTypeFromExtension() {
    // Arrange
    final UploadUserFileCommand command =
        new UploadUserFileCommand(USER_ID, "foto.PNG", null, new byte[] {1, 2, 3});

    when(getUserByIdPort.getById(new UserId(USER_ID))).thenReturn(Optional.of(existingUser()));
    when(userFileStoragePort.isAvailable()).thenReturn(true);
    when(userFileStoragePort.store(any(), any(), any(), any()))
        .thenReturn(new StoredFileMetadata("foto.PNG", "image/png", 3L, Instant.now()));

    // Act
    service.execute(command);

    // Assert
    verify(userFileStoragePort)
        .store(
            new UserId(USER_ID),
            UserFileName.from("foto.PNG"),
            new byte[] {1, 2, 3},
            UserFileContentType.IMAGE_PNG);
  }

  @Test
  @DisplayName("execute() lanza UserNotFoundException cuando el usuario no existe")
  void shouldThrowWhenUserDoesNotExist() {
    // Arrange
    final UploadUserFileCommand command =
        new UploadUserFileCommand(USER_ID, "certificado.pdf", "application/pdf", PDF_CONTENT);
    when(getUserByIdPort.getById(any())).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(UserNotFoundException.class, () -> service.execute(command));
    verifyNoInteractions(userFileStoragePort);
  }

  @Test
  @DisplayName("execute() rechaza archivos vacíos")
  void shouldRejectEmptyContent() {
    // Arrange
    final UploadUserFileCommand command =
        new UploadUserFileCommand(USER_ID, "vacio.pdf", "application/pdf", new byte[0]);
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(existingUser()));
    when(userFileStoragePort.isAvailable()).thenReturn(true);

    // Act & Assert
    assertThrows(InvalidUserFileException.class, () -> service.execute(command));
    verify(userFileStoragePort, never()).store(any(), any(), any(), any());
  }

  @Test
  @DisplayName("execute() rechaza archivos que superan el tamaño máximo")
  void shouldRejectOversizedFile() {
    // Arrange
    final byte[] oversized = new byte[(int) MAX_BYTES + 1];
    final UploadUserFileCommand command =
        new UploadUserFileCommand(USER_ID, "grande.pdf", "application/pdf", oversized);
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(existingUser()));
    when(userFileStoragePort.isAvailable()).thenReturn(true);

    // Act & Assert
    assertThrows(InvalidUserFileException.class, () -> service.execute(command));
    verify(userFileStoragePort, never()).store(any(), any(), any(), any());
  }

  @Test
  @DisplayName("execute() rechaza tipos que no sean imagen o PDF")
  void shouldRejectUnsupportedContentType() {
    // Arrange
    final UploadUserFileCommand command =
        new UploadUserFileCommand(USER_ID, "programa.exe", "application/x-msdownload", new byte[] {1});
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(existingUser()));
    when(userFileStoragePort.isAvailable()).thenReturn(true);

    // Act & Assert
    assertThrows(InvalidUserFileException.class, () -> service.execute(command));
    verify(userFileStoragePort, never()).store(any(), any(), any(), any());
  }

  @Test
  @DisplayName("execute() lanza UserFileStorageUnavailableException si B2 no está configurado")
  void shouldThrowWhenStorageIsUnavailable() {
    // Arrange
    final UploadUserFileCommand command =
        new UploadUserFileCommand(USER_ID, "certificado.pdf", "application/pdf", PDF_CONTENT);
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(existingUser()));
    when(userFileStoragePort.isAvailable()).thenReturn(false);

    // Act & Assert
    assertThrows(UserFileStorageUnavailableException.class, () -> service.execute(command));
    verify(userFileStoragePort, never()).store(any(), any(), any(), any());
  }

  private UserModel existingUser() {
    return new UserModel(
        new UserId(USER_ID),
        new UserName("John Arrieta"),
        new UserEmail("john@example.com"),
        UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuO"),
        UserRole.MEMBER,
        UserStatus.ACTIVE);
  }
}
