package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper;

import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserFileCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UploadUserFileCommand;
import com.jcaa.usersmanagement.application.service.dto.query.DownloadUserFileQuery;
import com.jcaa.usersmanagement.application.service.dto.query.ListUserFilesQuery;
import com.jcaa.usersmanagement.domain.exception.InvalidUserFileException;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserFileRestResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

/** Mapea entre los DTOs REST de archivos y los comandos/consultas/resultados de la aplicación. */
@UtilityClass
public class UserFileRestMapper {

  public UploadUserFileCommand toUploadCommand(
      final String userId, final MultipartFile multipartFile) {
    return new UploadUserFileCommand(
        userId,
        multipartFile.getOriginalFilename(),
        multipartFile.getContentType(),
        readContent(multipartFile));
  }

  public DownloadUserFileQuery toDownloadQuery(final String userId, final String fileName) {
    return new DownloadUserFileQuery(userId, fileName);
  }

  public ListUserFilesQuery toListQuery(final String userId) {
    return new ListUserFilesQuery(userId);
  }

  public DeleteUserFileCommand toDeleteCommand(final String userId, final String fileName) {
    return new DeleteUserFileCommand(userId, fileName);
  }

  public UserFileRestResponse toResponse(final StoredFileMetadata metadata) {
    return new UserFileRestResponse(
        metadata.fileName(),
        metadata.contentType(),
        metadata.size(),
        Objects.isNull(metadata.lastModified()) ? null : metadata.lastModified().toString());
  }

  public List<UserFileRestResponse> toResponseList(final List<StoredFileMetadata> files) {
    return files.stream().map(UserFileRestMapper::toResponse).toList();
  }

  private byte[] readContent(final MultipartFile multipartFile) {
    try {
      return multipartFile.getBytes();
    } catch (final IOException exception) {
      throw InvalidUserFileException.becauseContentCouldNotBeRead(exception);
    }
  }
}
