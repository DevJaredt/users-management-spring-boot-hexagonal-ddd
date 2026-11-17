package com.jcaa.search.infrastructure.entrypoint.rest.controller;

import com.jcaa.search.application.port.in.GetIndexedUserUseCase;
import com.jcaa.search.application.port.in.SearchUsersUseCase;
import com.jcaa.search.infrastructure.entrypoint.rest.dto.response.UserSearchPageRestResponse;
import com.jcaa.search.infrastructure.entrypoint.rest.dto.response.UserSearchRestResponse;
import com.jcaa.search.infrastructure.entrypoint.rest.mapper.UserSearchRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de lectura del CQRS (Guía 11).
 *
 * <p>Las consultas se resuelven directamente contra OpenSearch, nunca contra MySQL/TiDB. El índice
 * se mantiene al día con los eventos {@code create-user}, {@code update-user} y
 * {@code delete-user}.
 */
@Tag(name = "Search", description = "Búsquedas de usuarios sobre OpenSearch (CQRS).")
@RestController
@RequestMapping("/api/search/users")
public class UserSearchRestController {

  private final SearchUsersUseCase searchUsersUseCase;
  private final GetIndexedUserUseCase getIndexedUserUseCase;
  private final int defaultSize;

  public UserSearchRestController(
      final SearchUsersUseCase searchUsersUseCase,
      final GetIndexedUserUseCase getIndexedUserUseCase,
      @Value("${opensearch.search.default-size}") final int defaultSize) {
    this.searchUsersUseCase = searchUsersUseCase;
    this.getIndexedUserUseCase = getIndexedUserUseCase;
    this.defaultSize = defaultSize;
  }

  @Operation(summary = "Buscar usuarios por texto libre o listar los indexados")
  @GetMapping
  public UserSearchPageRestResponse search(
      @Parameter(description = "Texto libre: nombre, correo, rol o estado. Vacío devuelve todos.")
          @RequestParam(value = "q", required = false)
          final String text,
      @Parameter(description = "Página basada en cero.")
          @RequestParam(value = "page", defaultValue = "0")
          final int page,
      @Parameter(description = "Tamaño de página (máximo 100).")
          @RequestParam(value = "size", required = false)
          final Integer size) {
    final int pageSize = Objects.isNull(size) ? defaultSize : size;
    return UserSearchRestMapper.toPageResponse(
        searchUsersUseCase.execute(UserSearchRestMapper.toQuery(text, page, pageSize)),
        page,
        pageSize);
  }

  @Operation(summary = "Obtener un usuario desde el índice")
  @GetMapping("/{id}")
  public UserSearchRestResponse getById(@PathVariable final String id) {
    return UserSearchRestMapper.toResponse(getIndexedUserUseCase.execute(id));
  }
}
