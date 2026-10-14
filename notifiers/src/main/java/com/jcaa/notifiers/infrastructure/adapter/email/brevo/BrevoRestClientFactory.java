package com.jcaa.notifiers.infrastructure.adapter.email.brevo;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Construye el {@link RestClient} de Brevo con la autenticacion y los tipos de contenido que exige
 * la API.
 *
 * <p>Se aisla aqui para que la configuracion de Spring y los tests compartan exactamente las
 * mismas cabeceras por defecto (incluida {@code api-key}).
 */
@UtilityClass
public class BrevoRestClientFactory {

  private static final String API_KEY_HEADER = "api-key";

  public static RestClient.Builder builder(final BrevoConfig config) {
    return RestClient.builder()
        .baseUrl(config.baseUrl())
        .defaultHeader(API_KEY_HEADER, config.apiKey())
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
  }
}
