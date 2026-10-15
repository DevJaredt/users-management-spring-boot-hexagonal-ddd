package com.jcaa.notifiers.infrastructure.adapter.email.brevo;

import java.util.List;
import java.util.Map;

/** Cuerpo JSON que espera {@code POST /v3/smtp/email} de Brevo. */
record BrevoEmailRequest(
    Contact sender,
    List<Contact> to,
    String subject,
    String htmlContent,
    Map<String, String> headers) {

  record Contact(String email, String name) {}
}
