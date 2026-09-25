package com.jcaa.usersmanagement.infrastructure.adapter.email.template;

import com.jcaa.usersmanagement.application.port.out.dto.EmailTemplate;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;

/**
 * Traduce el tipo de notificacion que pide la aplicacion a la plantilla del classpath.
 *
 * <p>Mantiene las rutas de las plantillas fuera de la capa de aplicacion.
 */
@UtilityClass
public class EmailTemplateCatalog {

  private static final String BASE_PATH = "/templates/";
  private static final String HTML_EXTENSION = ".html";
  private static final String FILE_USER_CREATED = "user-created";
  private static final String FILE_USER_UPDATED = "user-updated";

  private static final Map<EmailTemplate, String> RESOURCE_BY_TEMPLATE =
      Map.of(
          EmailTemplate.USER_CREATED, BASE_PATH + FILE_USER_CREATED + HTML_EXTENSION,
          EmailTemplate.USER_UPDATED, BASE_PATH + FILE_USER_UPDATED + HTML_EXTENSION);

  public static String resourcePathFor(final EmailTemplate template) {
    Objects.requireNonNull(template, "El tipo de plantilla es requerido.");
    final String resourcePath = RESOURCE_BY_TEMPLATE.get(template);
    if (Objects.isNull(resourcePath)) {
      throw EmailSenderException.becauseSendFailed(
          new IllegalStateException("No hay plantilla asociada al tipo de notificacion."));
    }
    return resourcePath;
  }
}
