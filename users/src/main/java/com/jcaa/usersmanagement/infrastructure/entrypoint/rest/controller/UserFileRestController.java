package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.application.port.in.DeleteUserFileUseCase;
import com.jcaa.usersmanagement.application.port.in.DownloadUserFileUseCase;
import com.jcaa.usersmanagement.application.port.in.ListUserFilesUseCase;
import com.jcaa.usersmanagement.application.port.in.UploadUserFileUseCase;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFile;
import com.jcaa.usersmanagement.infrastructure.config.OpenApiConfig;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserFileRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper.UserFileRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de archivos por usuario (Guía 9).
 *
 * <p>Los objetos se guardan en el bucket privado de Backblaze B2 bajo {@code users/{userId}/}. La
 * descarga la realiza el microservicio después de validar que el usuario exista: el bucket nunca se
 * expone públicamente.
 */
@Tag(name = "User files", description = "Fotos y PDFs de cada usuario en Backblaze B2.")
@RestController
@RequestMapping("/api/users/{userId}/files")
@RequiredArgsConstructor
public class UserFileRestController {

  private static final String DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;

  private final UploadUserFileUseCase uploadUserFileUseCase;
  private final DownloadUserFileUseCase downloadUserFileUseCase;
  private final ListUserFilesUseCase listUserFilesUseCase;
  private final DeleteUserFileUseCase deleteUserFileUseCase;

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "Subir una foto o PDF del usuario")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UserFileRestResponse upload(
      @PathVariable final String userId, @RequestPart("file") final MultipartFile file) {
    return UserFileRestMapper.toResponse(
        uploadUserFileUseCase.execute(UserFileRestMapper.toUploadCommand(userId, file)));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "Listar los archivos del usuario")
  @GetMapping
  public List<UserFileRestResponse> list(@PathVariable final String userId) {
    return UserFileRestMapper.toResponseList(
        listUserFilesUseCase.execute(UserFileRestMapper.toListQuery(userId)));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "Descargar un archivo del usuario")
  @GetMapping("/{fileName:.+}")
  public ResponseEntity<ByteArrayResource> download(
      @PathVariable final String userId, @PathVariable final String fileName) {
    final StoredFile storedFile =
        downloadUserFileUseCase.execute(
            UserFileRestMapper.toDownloadQuery(userId, fileName));

    return ResponseEntity.ok()
        .contentType(mediaTypeOf(storedFile))
        .contentLength(storedFile.content().length)
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(storedFile))
        .body(new ByteArrayResource(storedFile.content()));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "Eliminar un archivo del usuario")
  @DeleteMapping("/{fileName:.+}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable final String userId, @PathVariable final String fileName) {
    deleteUserFileUseCase.execute(UserFileRestMapper.toDeleteCommand(userId, fileName));
  }

  private MediaType mediaTypeOf(final StoredFile storedFile) {
    final String contentType = storedFile.metadata().contentType();
    if (contentType == null || contentType.isBlank()) {
      return MediaType.parseMediaType(DEFAULT_MEDIA_TYPE);
    }
    return MediaType.parseMediaType(contentType);
  }

  private String contentDisposition(final StoredFile storedFile) {
    return ContentDisposition.attachment()
        .filename(storedFile.metadata().fileName(), StandardCharsets.UTF_8)
        .build()
        .toString();
  }
}
