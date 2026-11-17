package com.jcaa.search.domain.model;

/**
 * Documento de usuario indexado en OpenSearch (lado de lectura del CQRS).
 *
 * <p>No contiene contraseña ni datos sensibles: solo lo necesario para buscar y mostrar resultados.
 */
public record UserSearchDocument(
    String id, String name, String email, String role, String status) {}
