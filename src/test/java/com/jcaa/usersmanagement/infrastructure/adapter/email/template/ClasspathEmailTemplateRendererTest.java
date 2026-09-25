package com.jcaa.usersmanagement.infrastructure.adapter.email.template;

import static org.junit.jupiter.api.Assertions.*;

import com.jcaa.usersmanagement.application.port.out.dto.EmailTemplate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClasspathEmailTemplateRenderer")
class ClasspathEmailTemplateRendererTest {

  private static final String NAME = "John Arrieta";
  private static final String EMAIL = "john@example.com";
  private static final String ROLE = "ADMIN";
  private static final String STATUS = "ACTIVE";
  private static final String SAMPLE_PASSWORD = "SuperSecret1";

  private ClasspathEmailTemplateRenderer renderer;

  @BeforeEach
  void setUp() {
    renderer = new ClasspathEmailTemplateRenderer();
  }

  @Test
  @DisplayName("render() USER_CREATED sustituye los tokens y nunca expone la contrasena")
  void shouldRenderCreatedTemplateWithoutPassword() {
    // Act
    final String body =
        renderer.render(
            EmailTemplate.USER_CREATED,
            Map.of("name", NAME, "email", EMAIL, "role", ROLE, "password", SAMPLE_PASSWORD));

    // Assert
    assertAll(
        "plantilla de creacion",
        () -> assertTrue(body.contains(NAME), "debe incluir el nombre"),
        () -> assertTrue(body.contains(EMAIL), "debe incluir el email"),
        () -> assertTrue(body.contains(ROLE), "debe incluir el rol"),
        () -> assertFalse(body.contains("{{"), "no deben quedar tokens sin sustituir"),
        () ->
            assertFalse(
                body.contains(SAMPLE_PASSWORD),
                "la plantilla no debe renderizar la contrasena aunque se reciba como variable"));
  }

  @Test
  @DisplayName("render() USER_UPDATED incluye el estado de la cuenta")
  void shouldRenderUpdatedTemplateWithStatus() {
    // Act
    final String body =
        renderer.render(
            EmailTemplate.USER_UPDATED,
            Map.of("name", NAME, "email", EMAIL, "role", ROLE, "status", STATUS));

    // Assert
    assertAll(
        "plantilla de actualizacion",
        () -> assertTrue(body.contains(STATUS), "debe incluir el estado"),
        () -> assertFalse(body.contains("{{"), "no deben quedar tokens sin sustituir"));
  }

  @Test
  @DisplayName("render() escapa el HTML de las variables para evitar inyeccion")
  void shouldEscapeHtmlInVariables() {
    // Act
    final String body =
        renderer.render(
            EmailTemplate.USER_CREATED,
            Map.of("name", "<script>alert(1)</script>", "email", EMAIL, "role", ROLE));

    // Assert
    assertAll(
        "escape de HTML",
        () -> assertFalse(body.contains("<script>"), "no debe inyectarse la etiqueta"),
        () -> assertTrue(body.contains("&lt;script&gt;"), "debe quedar escapada"));
  }

  @Test
  @DisplayName("render() reutiliza la plantilla cacheada entre llamadas")
  void shouldReuseCachedTemplate() {
    // Act
    final String first =
        renderer.render(
            EmailTemplate.USER_CREATED, Map.of("name", NAME, "email", EMAIL, "role", ROLE));
    final String second =
        renderer.render(
            EmailTemplate.USER_CREATED,
            Map.of("name", "Otra Persona", "email", EMAIL, "role", ROLE));

    // Assert
    assertAll(
        "cache de plantillas",
        () -> assertNotEquals(first, second, "cada render debe aplicar sus propias variables"),
        () -> assertTrue(second.contains("Otra Persona"), "debe usar la nueva variable"));
  }
}
