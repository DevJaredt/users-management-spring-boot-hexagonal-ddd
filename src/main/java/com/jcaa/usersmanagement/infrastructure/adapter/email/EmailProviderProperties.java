package com.jcaa.usersmanagement.infrastructure.adapter.email;

import lombok.experimental.UtilityClass;

/**
 * Vocabulario de configuracion del proveedor de correo.
 *
 * <p>La implementacion activa del puerto {@code EmailSenderPort} se elige con la propiedad
 * {@code email.provider}, de modo que cambiar de proveedor no obligue a tocar el nucleo.
 */
@UtilityClass
public class EmailProviderProperties {

  public static final String PROVIDER = "email.provider";

  /** SMTP clasico con javax.mail. Requiere plan Pro en Railway. */
  public static final String JAVAMAIL = "javamail";

  /** API HTTPS de Resend. Funciona en todos los planes de Railway. */
  public static final String RESEND = "resend";
}
