package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.application.port.out.dto.UserEvent;

/**
 * Puerto de salida para publicar cambios de usuario (paso 2 del diagrama: asincrono).
 */
public interface UserEventPublisherPort {

  void publish(UserEvent event);
}
