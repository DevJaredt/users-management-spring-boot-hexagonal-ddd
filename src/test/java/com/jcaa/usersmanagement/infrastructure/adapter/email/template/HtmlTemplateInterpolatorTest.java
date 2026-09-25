package com.jcaa.usersmanagement.infrastructure.adapter.email.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HtmlTemplateInterpolator")
class HtmlTemplateInterpolatorTest {

  @Test
  @DisplayName("interpolate() sustituye todos los tokens presentes")
  void shouldReplaceAllTokens() {
    // Act
    final String result =
        HtmlTemplateInterpolator.interpolate(
            "<p>{{name}} - {{email}}</p>", Map.of("name", "Ana", "email", "ana@example.com"));

    // Assert
    assertEquals("<p>Ana - ana@example.com</p>", result);
  }

  @Test
  @DisplayName("interpolate() deja intactos los tokens sin variable")
  void shouldLeaveUnknownTokensUntouched() {
    // Act
    final String result =
        HtmlTemplateInterpolator.interpolate("<p>{{known}} {{unknown}}</p>", Map.of("known", "ok"));

    // Assert
    assertEquals("<p>ok {{unknown}}</p>", result);
  }

  @Test
  @DisplayName("interpolate() escapa el HTML de los valores")
  void shouldEscapeHtmlValues() {
    // Act
    final String result =
        HtmlTemplateInterpolator.interpolate("<p>{{name}}</p>", Map.of("name", "<b>Ana</b>"));

    // Assert
    assertEquals("<p>&lt;b&gt;Ana&lt;/b&gt;</p>", result);
  }

  @Test
  @DisplayName("escapeHtml() escapa ampersand, etiquetas y comillas sin doble escapado")
  void shouldEscapeAllSpecialCharacters() {
    // Act
    final String escaped = HtmlTemplateInterpolator.escapeHtml("&<>\"'");

    // Assert
    assertEquals("&amp;&lt;&gt;&quot;&#39;", escaped);
  }
}
