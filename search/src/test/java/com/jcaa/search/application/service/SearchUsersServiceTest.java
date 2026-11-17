package com.jcaa.search.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.search.application.port.out.UserSearchIndexPort;
import com.jcaa.search.application.service.dto.query.SearchUsersQuery;
import com.jcaa.search.domain.exception.SearchUnavailableException;
import com.jcaa.search.domain.model.UserSearchDocument;
import com.jcaa.search.domain.model.UserSearchResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests para SearchUsersService (Guía 11: lectura desde OpenSearch). */
@DisplayName("SearchUsersService")
@ExtendWith(MockitoExtension.class)
class SearchUsersServiceTest {

  @Mock private UserSearchIndexPort userSearchIndexPort;

  private SearchUsersService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service = new SearchUsersService(userSearchIndexPort, validatorFactory.getValidator());
    }
  }

  @Test
  @DisplayName("execute() consulta el índice cuando OpenSearch está disponible")
  void shouldSearchWhenAvailable() {
    // Arrange
    final UserSearchResult expected =
        new UserSearchResult(
            List.of(new UserSearchDocument("u-1", "John", "john@example.com", "ADMIN", "ACTIVE")),
            1);
    when(userSearchIndexPort.isAvailable()).thenReturn(true);
    when(userSearchIndexPort.search("john", 0, 10)).thenReturn(expected);

    // Act
    final UserSearchResult result = service.execute(new SearchUsersQuery("john", 0, 10));

    // Assert
    assertEquals(1, result.total());
    assertEquals("u-1", result.users().get(0).id());
  }

  @Test
  @DisplayName("execute() normaliza el texto vacío para listar todos")
  void shouldNormalizeBlankText() {
    // Arrange
    when(userSearchIndexPort.isAvailable()).thenReturn(true);
    when(userSearchIndexPort.search("", 0, 20))
        .thenReturn(new UserSearchResult(List.of(), 0));

    // Act
    service.execute(new SearchUsersQuery("   ", 0, 20));

    // Assert
    verify(userSearchIndexPort).search("", 0, 20);
  }

  @Test
  @DisplayName("execute() lanza SearchUnavailableException si OpenSearch no está configurado")
  void shouldThrowWhenUnavailable() {
    // Arrange
    when(userSearchIndexPort.isAvailable()).thenReturn(false);

    // Act & Assert
    assertThrows(
        SearchUnavailableException.class,
        () -> service.execute(new SearchUsersQuery("john", 0, 10)));
    verify(userSearchIndexPort).isAvailable();
    verify(userSearchIndexPort, never()).search(anyString(), anyInt(), anyInt());
  }

  @Test
  @DisplayName("execute() valida el tamaño de página antes de consultar")
  void shouldThrowWhenQueryIsInvalid() {
    // Act & Assert
    assertThrows(
        ConstraintViolationException.class,
        () -> service.execute(new SearchUsersQuery("john", -1, 0)));
    verify(userSearchIndexPort, never()).search(anyString(), anyInt(), anyInt());
  }
}
