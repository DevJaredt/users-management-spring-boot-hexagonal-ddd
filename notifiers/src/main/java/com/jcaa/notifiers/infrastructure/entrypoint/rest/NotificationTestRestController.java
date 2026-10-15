package com.jcaa.notifiers.infrastructure.entrypoint.rest;

import com.jcaa.notifiers.application.service.UserNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de <b>prueba</b> para disparar un correo por el relay SMTP de Brevo sin pasar por Kafka.
 *
 * <p>El flujo real de notificaciones es asincrono ({@code user-events/create-account} -&gt; Kafka
 * -&gt; notifiers -&gt; SMTP). Este endpoint existe unicamente para poder probar la conexion SMTP
 * desde Swagger UI.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationTestRestController {

  private final UserNotificationService userNotificationService;

  @PostMapping("/test-email")
  public ResponseEntity<Void> sendTestEmail(@Valid @RequestBody final TestEmailRequest request) {
    userNotificationService.notifyTest(request.name(), request.email());
    return ResponseEntity.accepted().build();
  }

  /** Destinatario del correo de prueba. */
  public record TestEmailRequest(
      @NotBlank(message = "email es requerido")
          @Email(message = "email debe ser una direccion valida")
          String email,
      String name) {}
}
