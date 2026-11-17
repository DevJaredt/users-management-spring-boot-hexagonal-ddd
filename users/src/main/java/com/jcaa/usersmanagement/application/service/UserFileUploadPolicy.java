package com.jcaa.usersmanagement.application.service;

/**
 * Política de subida de archivos.
 *
 * @param maxBytes tamaño máximo permitido para un archivo de usuario
 */
public record UserFileUploadPolicy(long maxBytes) {}
