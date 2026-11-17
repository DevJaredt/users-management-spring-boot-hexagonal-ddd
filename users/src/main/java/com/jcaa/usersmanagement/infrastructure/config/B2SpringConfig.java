package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.infrastructure.adapter.storage.b2.B2FileStorageAdapter;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Cliente S3 apuntando al endpoint compatible de Backblaze B2 (Guía 9).
 *
 * <p>Se usa {@code path-style} porque B2 expone el bucket en la ruta y no como subdominio. El bucket
 * es privado y las credenciales son una Application Key restringida a ese bucket.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "b2.enabled", havingValue = "true")
public class B2SpringConfig {

  private static final String LOG_CREDENTIALS_MISSING =
      "B2_ENABLED=true requiere B2_BUCKET, B2_KEY_ID y B2_APPLICATION_KEY.";

  @Bean(destroyMethod = "close")
  public S3Client b2S3Client(
      @Value("${b2.endpoint}") final String endpoint,
      @Value("${b2.region}") final String region,
      @Value("${b2.key-id}") final String keyId,
      @Value("${b2.application-key}") final String applicationKey,
      @Value("${b2.connect-timeout}") final java.time.Duration connectTimeout,
      @Value("${b2.read-timeout}") final java.time.Duration readTimeout) {
    validateCredentials(keyId, applicationKey);

    final SdkHttpClient httpClient =
        UrlConnectionHttpClient.builder()
            .connectionTimeout(connectTimeout)
            .socketTimeout(readTimeout)
            .build();

    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(keyId, applicationKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .httpClient(httpClient)
        .build();
  }

  @Bean
  public UserFileStoragePort b2FileStoragePort(
      final S3Client b2S3Client, @Value("${b2.bucket}") final String bucket) {
    if (bucket == null || bucket.isBlank()) {
      throw new IllegalStateException(LOG_CREDENTIALS_MISSING);
    }
    return new B2FileStorageAdapter(b2S3Client, bucket);
  }

  private void validateCredentials(final String keyId, final String applicationKey) {
    if (keyId == null
        || keyId.isBlank()
        || applicationKey == null
        || applicationKey.isBlank()) {
      throw new IllegalStateException(LOG_CREDENTIALS_MISSING);
    }
  }
}
