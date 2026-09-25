package com.jcaa.usersmanagement.infrastructure.adapter.email.resend;

import java.util.List;

/**
 * Cuerpo JSON que espera el endpoint {@code POST /emails} de Resend.
 */
public record ResendEmailRequest(String from, List<String> to, String subject, String html) {}
