package com.jcaa.usersmanagement.infrastructure.adapter.email.template;

import com.jcaa.usersmanagement.application.port.out.dto.EmailTemplate;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Carga las plantillas HTML desde el classpath, las cachea y las renderiza.
 *
 * <p>Vive en infraestructura: el acceso a recursos y el render son detalles de implementacion que
 * la capa de aplicacion no debe conocer.
 */
@Component
public class ClasspathEmailTemplateRenderer {

  private static final String ERROR_TEMPLATE_NOT_FOUND = "Plantilla de correo no encontrada: %s";

  private final Map<String, String> templateCache = new ConcurrentHashMap<>();

  public String render(final EmailTemplate template, final Map<String, String> variables) {
    final String resourcePath = EmailTemplateCatalog.resourcePathFor(template);
    final String source =
        templateCache.computeIfAbsent(
            resourcePath, ClasspathEmailTemplateRenderer::readTemplate);
    return HtmlTemplateInterpolator.interpolate(source, variables);
  }

  private static String readTemplate(final String resourcePath) {
    try (InputStream inputStream =
        ClasspathEmailTemplateRenderer.class.getResourceAsStream(resourcePath)) {
      if (Objects.isNull(inputStream)) {
        throw EmailSenderException.becauseSendFailed(
            new IllegalStateException(String.format(ERROR_TEMPLATE_NOT_FOUND, resourcePath)));
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException ioException) {
      throw EmailSenderException.becauseSendFailed(ioException);
    }
  }
}
