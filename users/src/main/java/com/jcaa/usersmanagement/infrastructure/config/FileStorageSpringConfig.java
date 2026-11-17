package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.application.port.out.UserFileStoragePort;
import com.jcaa.usersmanagement.application.service.UserFileUploadPolicy;
import com.jcaa.usersmanagement.infrastructure.adapter.storage.UnavailableUserFileStorageAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Política de subida y adaptador de respaldo cuando no hay proveedor de archivos configurado.
 *
 * <p>La Guía 9 es opcional en local: con {@code B2_ENABLED=false} la aplicación arranca y los
 * endpoints de archivos responden 503. Con {@code B2_ENABLED=true} se activa
 * {@link B2SpringConfig}.
 */
@Configuration(proxyBeanMethods = false)
public class FileStorageSpringConfig {

  @Bean
  public UserFileUploadPolicy userFileUploadPolicy(
      @Value("${b2.max-file-size-bytes}") final long maxFileSizeBytes) {
    return new UserFileUploadPolicy(maxFileSizeBytes);
  }

  @Bean
  @ConditionalOnProperty(name = "b2.enabled", havingValue = "false", matchIfMissing = true)
  public UserFileStoragePort unavailableUserFileStoragePort() {
    return new UnavailableUserFileStorageAdapter();
  }
}
