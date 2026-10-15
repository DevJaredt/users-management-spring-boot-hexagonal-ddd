package com.jcaa.usersmanagement.infrastructure.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia el microservicio {@code accounts} (paso 5 del diagrama).
 *
 * <p>Los timeouts son explicitos para que una caida de accounts no bloquee el hilo de la peticion
 * HTTP del servicio users.
 */
@Configuration(proxyBeanMethods = false)
public class AccountsSpringConfig {

  @Bean
  public RestClient accountsRestClient(
      @Value("${accounts.base-url}") final String baseUrl,
      @Value("${accounts.timeout.connect}") final Duration connectTimeout,
      @Value("${accounts.timeout.read}") final Duration readTimeout) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }
}
