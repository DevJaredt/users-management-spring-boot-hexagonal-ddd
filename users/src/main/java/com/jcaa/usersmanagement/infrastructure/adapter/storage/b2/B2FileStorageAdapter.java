package com.jcaa.usersmanagement.infrastructure.adapter.storage.b2;

import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFile;
import com.jcaa.usersmanagement.application.port.out.dto.StoredFileMetadata;
import com.jcaa.usersmanagement.domain.enums.UserFileContentType;
import com.jcaa.usersmanagement.domain.valueobject.UserFileName;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.infrastructure.adapter.storage.exception.FileStorageException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Adaptador S3 para el bucket privado de Backblaze B2 (Guía 9).
 *
 * <p>Cada usuario tiene su propia "carpeta" lógica: {@code users/{userId}/{fileName}}. El bucket es
 * privado, por lo que todas las operaciones se autentican con la Application Key y nunca se exponen
 * URLs públicas.
 */
@Slf4j
@RequiredArgsConstructor
public class B2FileStorageAdapter implements UserFileStoragePort {

  private static final String KEY_PREFIX = "users/";
  private static final String KEY_SEPARATOR = "/";
  private static final int HTTP_NOT_FOUND = 404;
  private static final String LOG_UPLOADED =
      "[B2FileStorageAdapter] archivo subido. userId={} fileName={} bytes={}";
  private static final String LOG_DELETED =
      "[B2FileStorageAdapter] archivo eliminado. userId={} fileName={}";

  private final S3Client s3Client;
  private final String bucket;

  @Override
  public StoredFileMetadata store(
      final UserId ownerId,
      final UserFileName fileName,
      final byte[] content,
      final UserFileContentType contentType) {
    final String key = buildKey(ownerId, fileName);
    final PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType.mimeType())
            .contentLength((long) content.length)
            .build();
    try {
      s3Client.putObject(request, RequestBody.fromBytes(content));
      log.info(LOG_UPLOADED, ownerId.value(), fileName.value(), content.length);
      return new StoredFileMetadata(
          fileName.value(), contentType.mimeType(), content.length, Instant.now());
    } catch (final S3Exception exception) {
      throw FileStorageException.becauseUploadFailed(fileName.value(), exception);
    }
  }

  @Override
  public Optional<StoredFile> load(final UserId ownerId, final UserFileName fileName) {
    final GetObjectRequest request =
        GetObjectRequest.builder().bucket(bucket).key(buildKey(ownerId, fileName)).build();
    try {
      final ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
      final GetObjectResponse metadata = response.response();
      final StoredFileMetadata fileMetadata =
          new StoredFileMetadata(
              fileName.value(),
              metadata.contentType(),
              response.asByteArray().length,
              metadata.lastModified());
      return Optional.of(new StoredFile(fileMetadata, response.asByteArray()));
    } catch (final NoSuchKeyException exception) {
      return Optional.empty();
    } catch (final S3Exception exception) {
      if (exception.statusCode() == HTTP_NOT_FOUND) {
        return Optional.empty();
      }
      throw FileStorageException.becauseDownloadFailed(fileName.value(), exception);
    }
  }

  @Override
  public List<StoredFileMetadata> list(final UserId ownerId) {
    final String prefix = buildPrefix(ownerId);
    final ListObjectsV2Request request =
        ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
    try {
      final ListObjectsV2Response response = s3Client.listObjectsV2(request);
      return response.contents().stream()
          .filter(object -> !object.key().equals(prefix))
          .map(object -> toMetadata(prefix, object))
          .toList();
    } catch (final S3Exception exception) {
      throw FileStorageException.becauseListFailed(exception);
    }
  }

  @Override
  public void delete(final UserId ownerId, final UserFileName fileName) {
    final DeleteObjectRequest request =
        DeleteObjectRequest.builder().bucket(bucket).key(buildKey(ownerId, fileName)).build();
    try {
      s3Client.deleteObject(request);
      log.info(LOG_DELETED, ownerId.value(), fileName.value());
    } catch (final S3Exception exception) {
      if (exception.statusCode() == HTTP_NOT_FOUND) {
        return;
      }
      throw FileStorageException.becauseDeleteFailed(fileName.value(), exception);
    }
  }

  @Override
  public boolean isAvailable() {
    return bucket != null && !bucket.isBlank();
  }

  private StoredFileMetadata toMetadata(final String prefix, final S3Object object) {
    final String fileName = object.key().substring(prefix.length());
    return new StoredFileMetadata(
        fileName, resolveContentType(object.key()), object.size(), object.lastModified());
  }

  /** ListObjectsV2 no devuelve el Content-Type: se consulta con HEAD (pocos archivos por usuario). */
  private String resolveContentType(final String key) {
    try {
      final HeadObjectResponse head =
          s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
      return head.contentType();
    } catch (final S3Exception exception) {
      return null;
    }
  }

  private String buildPrefix(final UserId ownerId) {
    return KEY_PREFIX + ownerId.value() + KEY_SEPARATOR;
  }

  private String buildKey(final UserId ownerId, final UserFileName fileName) {
    return buildPrefix(ownerId) + fileName.value();
  }
}
