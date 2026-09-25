package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.application.port.out.dto.EmailNotificationRequest;
import com.jcaa.usersmanagement.application.port.out.dto.EmailTemplate;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Decide QUE notificar cuando ocurre un cambio de usuario.
 *
 * <p>No conoce HTML ni SMTP: compone la intencion de negocio y delega en {@link EmailSenderPort}.
 * El envio es asincrono para no penalizar la latencia de la peticion HTTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

  private static final String SUBJECT_CREATED = "Tu cuenta ha sido creada — Gestion de Usuarios";
  private static final String SUBJECT_UPDATED =
      "Tu cuenta ha sido actualizada — Gestion de Usuarios";

  private static final String TOKEN_NAME = "name";
  private static final String TOKEN_EMAIL = "email";
  private static final String TOKEN_ROLE = "role";
  private static final String TOKEN_STATUS = "status";

  private static final String LOG_SEND_FAILED =
      "[EmailNotificationService] correo no enviado. Causa: {}";

  private final EmailSenderPort emailSenderPort;

  @Async
  public void notifyUserCreated(final UserModel user) {
    final EmailNotificationRequest request =
        new EmailNotificationRequest(
            user.getEmail().value(),
            user.getName().value(),
            SUBJECT_CREATED,
            EmailTemplate.USER_CREATED,
            Map.of(
                TOKEN_NAME, user.getName().value(),
                TOKEN_EMAIL, user.getEmail().value(),
                TOKEN_ROLE, user.getRole().name()));
    sendOrLog(request);
  }

  @Async
  public void notifyUserUpdated(final UserModel user) {
    final EmailNotificationRequest request =
        new EmailNotificationRequest(
            user.getEmail().value(),
            user.getName().value(),
            SUBJECT_UPDATED,
            EmailTemplate.USER_UPDATED,
            Map.of(
                TOKEN_NAME, user.getName().value(),
                TOKEN_EMAIL, user.getEmail().value(),
                TOKEN_ROLE, user.getRole().name(),
                TOKEN_STATUS, user.getStatus().name()));
    sendOrLog(request);
  }

  private void sendOrLog(final EmailNotificationRequest request) {
    try {
      emailSenderPort.send(request);
    } catch (final EmailSenderException senderException) {
      // El correo es un efecto secundario: un fallo SMTP no debe tumbar la operacion
      // de negocio que ya fue persistida.
      log.warn(LOG_SEND_FAILED, senderException.getMessage(), senderException);
    }
  }
}
