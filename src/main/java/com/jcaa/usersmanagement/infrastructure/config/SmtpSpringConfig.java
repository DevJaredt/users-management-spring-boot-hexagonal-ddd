package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.email.EmailProviderProperties;
import com.jcaa.usersmanagement.infrastructure.adapter.email.SmtpConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    name = EmailProviderProperties.PROVIDER,
    havingValue = EmailProviderProperties.JAVAMAIL,
    matchIfMissing = true)
public class SmtpSpringConfig {

  private static final String PROP_SMTP_HOST        = "${smtp.host}";
  private static final String PROP_SMTP_PORT        = "${smtp.port}";
  private static final String PROP_SMTP_USERNAME    = "${smtp.username}";
  private static final String PROP_SMTP_PASSWORD    = "${smtp.password}";
  private static final String PROP_SMTP_FROM        = "${smtp.from.address}";
  private static final String PROP_SMTP_FROM_NAME   = "${smtp.from.name}";
  private static final String PROP_SMTP_TIMEOUT     = "${smtp.timeout.millis}";

  @Value(PROP_SMTP_HOST)
  private String smtpHost;

  @Value(PROP_SMTP_PORT)
  private int smtpPort;

  @Value(PROP_SMTP_USERNAME)
  private String smtpUsername;

  @Value(PROP_SMTP_PASSWORD)
  private String smtpPassword;

  @Value(PROP_SMTP_FROM)
  private String smtpFromAddress;

  @Value(PROP_SMTP_FROM_NAME)
  private String smtpFromName;

  @Value(PROP_SMTP_TIMEOUT)
  private int smtpTimeoutMillis;

  @Bean
  public SmtpConfig smtpConfig() {
    return new SmtpConfig(
        smtpHost,
        smtpPort,
        smtpUsername,
        smtpPassword,
        smtpFromAddress,
        smtpFromName,
        smtpTimeoutMillis);
  }
}
