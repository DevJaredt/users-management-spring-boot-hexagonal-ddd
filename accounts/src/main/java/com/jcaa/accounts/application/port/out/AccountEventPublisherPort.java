package com.jcaa.accounts.application.port.out;

import com.jcaa.accounts.domain.model.AccountModel;

/**
 * Puerto de salida: publica el evento de cuenta creada en Kafka (paso 7 del diagrama).
 *
 * <p>La aplicacion decide QUE ocurrio; la infraestructura decide como y donde se publica.
 */
public interface AccountEventPublisherPort {

  void publish(AccountModel account);
}
