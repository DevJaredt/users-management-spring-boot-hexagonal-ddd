package com.jcaa.usersmanagement.infrastructure.adapter.email.resend;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.application.port.out.dto.EmailNotificationRequest;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.infrastructure.adapter.email.EmailProviderProperties;
import com.jcaa.usersmanagement.infrastructure.adapter.email.template.ClasspathEmailTemplateRenderer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implementacion del puerto de correo sobre la API HTTPS de Resend.
 *
 * <p>Se usa cuando {@code email.provider=resend}. Va por el puerto 443, por lo que funciona en
 * planes de Railway donde el SMTP saliente esta deshabilitado. El mensaje de error nunca incluye
 * el cuerpo de la respuesta del proveedor para no filtrar datos personales a los logs.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = EmailProviderProperties.PROVIDER,
    havingValue = EmailProviderProperties.RESEND)
public class ResendEmailSenderAdapter implements EmailSenderPort {

  private static final String SEND_EMAIL_PATH = "/emails";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String FROM_FORMAT = "%s <%s>";
  private static final String HTTP_ERROR_FORMAT = "HTTP %d %s";
  private static final String LOG_SENT = "[ResendEmailSenderAdapter] correo enviado exitosamente.";

  private final RestClient restClient;
  private final ResendConfig config;
  private final ClasspathEmailTemplateRenderer templateRenderer;

  public ResendEmailSenderAdapter(
      final RestClient resendRestClient,
      final ResendConfig config,
      final ClasspathEmailTemplateRenderer templateRenderer) {
    this.restClient = resendRestClient;
    this.config = config;
    this.templateRenderer = templateRenderer;
  }

  @Override
  public void send(final EmailNotificationRequest request) {
    try {
      restClient
          .post()
          .uri(SEND_EMAIL_PATH)
          .header(AUTHORIZATION_HEADER, BEARER_PREFIX + config.apiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(buildRequestBody(request))
          .retrieve()
          .toBodilessEntity();
      log.info(LOG_SENT);
    } catch (final RestClientResponseException responseException) {
      throw EmailSenderException.becauseProviderFailed(
          String.format(
              HTTP_ERROR_FORMAT,
              responseException.getStatusCode().value(),
              responseException.getStatusText()));
    } catch (final RestClientException clientException) {
      throw EmailSenderException.becauseProviderFailed(clientException.getMessage());
    }
  }

  private ResendEmailRequest buildRequestBody(final EmailNotificationRequest request) {
    final String html = templateRenderer.render(request.template(), request.variables());
    return new ResendEmailRequest(
        String.format(FROM_FORMAT, config.fromName(), config.fromAddress()),
        List.of(request.recipientEmail()),
        request.subject(),
        html);
  }
}
