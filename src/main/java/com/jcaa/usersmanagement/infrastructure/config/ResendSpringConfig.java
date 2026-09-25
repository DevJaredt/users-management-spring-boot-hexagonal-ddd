package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.email.EmailProviderProperties;
import com.jcaa.usersmanagement.infrastructure.adapter.email.resend.ResendConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cablea el adaptador de correo de Resend cuando {@code email.provider=resend}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = EmailProviderProperties.PROVIDER,
    havingValue = EmailProviderProperties.RESEND)
public class ResendSpringConfig {

  private static final String PROP_RESEND_API_KEY      = "${resend.api-key}";
  private static final String PROP_RESEND_FROM_ADDRESS = "${resend.from.address}";
  private static final String PROP_RESEND_FROM_NAME    = "${resend.from.name}";
  private static final String PROP_RESEND_BASE_URL     = "${resend.base-url}";
  private static final String PROP_RESEND_TIMEOUT      = "${resend.timeout.millis}";

  @Value(PROP_RESEND_API_KEY)
  private String apiKey;

  @Value(PROP_RESEND_FROM_ADDRESS)
  private String fromAddress;

  @Value(PROP_RESEND_FROM_NAME)
  private String fromName;

  @Value(PROP_RESEND_BASE_URL)
  private String baseUrl;

  @Value(PROP_RESEND_TIMEOUT)
  private int timeoutMillis;

  @Bean
  public ResendConfig resendConfig() {
    return new ResendConfig(apiKey, fromAddress, fromName, baseUrl, timeoutMillis);
  }

  @Bean
  public RestClient resendRestClient(final ResendConfig resendConfig) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(resendConfig.timeoutMillis());
    requestFactory.setReadTimeout(resendConfig.timeoutMillis());
    return RestClient.builder()
        .baseUrl(resendConfig.baseUrl())
        .requestFactory(requestFactory)
        .build();
  }
}
