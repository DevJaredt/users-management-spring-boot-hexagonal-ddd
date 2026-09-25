package com.jcaa.usersmanagement.infrastructure.adapter.email.template;

import java.util.Map;
import lombok.experimental.UtilityClass;

/**
 * Sustituye los tokens {@code {{clave}}} de una plantilla HTML.
 *
 * <p>Los valores se escapan antes de interpolarse para evitar inyeccion de HTML en el cuerpo del
 * correo (por ejemplo un nombre de usuario que contenga etiquetas).
 */
@UtilityClass
public class HtmlTemplateInterpolator {

  private static final String TOKEN_PREFIX = "{{";
  private static final String TOKEN_SUFFIX = "}}";

  private static final String AMPERSAND = "&";
  private static final String AMPERSAND_ENTITY = "&amp;";
  private static final String LESS_THAN = "<";
  private static final String LESS_THAN_ENTITY = "&lt;";
  private static final String GREATER_THAN = ">";
  private static final String GREATER_THAN_ENTITY = "&gt;";
  private static final String DOUBLE_QUOTE = "\"";
  private static final String DOUBLE_QUOTE_ENTITY = "&quot;";
  private static final String SINGLE_QUOTE = "'";
  private static final String SINGLE_QUOTE_ENTITY = "&#39;";

  public static String interpolate(final String template, final Map<String, String> variables) {
    String result = template;
    for (final Map.Entry<String, String> variable : variables.entrySet()) {
      final String token = TOKEN_PREFIX + variable.getKey() + TOKEN_SUFFIX;
      result = result.replace(token, escapeHtml(variable.getValue()));
    }
    return result;
  }

  /** El ampersand se reemplaza primero para no escapar dos veces las entidades generadas. */
  public static String escapeHtml(final String value) {
    return value
        .replace(AMPERSAND, AMPERSAND_ENTITY)
        .replace(LESS_THAN, LESS_THAN_ENTITY)
        .replace(GREATER_THAN, GREATER_THAN_ENTITY)
        .replace(DOUBLE_QUOTE, DOUBLE_QUOTE_ENTITY)
        .replace(SINGLE_QUOTE, SINGLE_QUOTE_ENTITY);
  }
}
