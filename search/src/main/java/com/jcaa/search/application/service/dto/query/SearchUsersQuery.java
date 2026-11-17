package com.jcaa.search.application.service.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Consulta de búsqueda.
 *
 * @param text texto libre; vacío devuelve todos los usuarios indexados
 * @param page página basada en cero
 * @param size tamaño de página
 */
public record SearchUsersQuery(
    @Size(max = 120, message = "q must have at most 120 characters") String text,
    @Min(value = 0, message = "page must be greater than or equal to 0") int page,
    @Min(value = 1, message = "size must be greater than or equal to 1")
        @Max(value = 100, message = "size must be less than or equal to 100")
        int size) {}
