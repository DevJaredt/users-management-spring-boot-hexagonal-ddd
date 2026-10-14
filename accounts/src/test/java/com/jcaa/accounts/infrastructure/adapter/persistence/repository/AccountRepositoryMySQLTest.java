package com.jcaa.accounts.infrastructure.adapter.persistence.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.infrastructure.adapter.persistence.exception.AccountPersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AccountRepositoryMySQL")
@ExtendWith(MockitoExtension.class)
class AccountRepositoryMySQLTest {

  private static final String ID = "acc-1";
  private static final String USER_ID = "user-1";
  private static final String NAME = "Jared";
  private static final String EMAIL = "jared@example.com";

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private PreparedStatement statement;
  @Mock private ResultSet resultSet;

  private AccountRepositoryMySQL repository;

  @BeforeEach
  void setUp() {
    repository = new AccountRepositoryMySQL(dataSource);
  }

  private void wireConnection() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
  }

  private void stubRow() throws SQLException {
    when(resultSet.getString("id")).thenReturn(ID);
    when(resultSet.getString("user_id")).thenReturn(USER_ID);
    when(resultSet.getString("name")).thenReturn(NAME);
    when(resultSet.getString("email")).thenReturn(EMAIL);
  }

  @Test
  @DisplayName("save() es idempotente: si la cuenta ya existe devuelve la existente sin insertar")
  void shouldReturnExistingAccountWithoutInserting() throws SQLException {
    // Arrange
    wireConnection();
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    stubRow();

    // Act
    final AccountModel result = repository.save(new AccountModel("otro-id", USER_ID, NAME, EMAIL));

    // Assert
    assertAll(
        "cuenta existente",
        () -> assertEquals(ID, result.id(), "debe conservar el id existente"),
        () -> assertEquals(USER_ID, result.userId()));
    verify(statement, never()).executeUpdate();
  }

  @Test
  @DisplayName("save() inserta cuando no existe y devuelve la cuenta persistida")
  void shouldInsertWhenAccountDoesNotExist() throws SQLException {
    // Arrange
    wireConnection();
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false, true);
    stubRow();

    // Act
    final AccountModel result = repository.save(new AccountModel(ID, USER_ID, NAME, EMAIL));

    // Assert
    assertAll(
        "cuenta insertada",
        () -> assertEquals(ID, result.id()),
        () -> assertEquals(EMAIL, result.email()));
    verify(statement).executeUpdate();
  }

  @Test
  @DisplayName("getByUserId() devuelve la cuenta cuando existe")
  void shouldReturnAccountWhenFound() throws SQLException {
    // Arrange
    wireConnection();
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    stubRow();

    // Act
    final Optional<AccountModel> result = repository.getByUserId(USER_ID);

    // Assert
    assertTrue(result.isPresent(), "debe estar presente");
  }

  @Test
  @DisplayName("getByUserId() devuelve vacio cuando no existe")
  void shouldReturnEmptyWhenNotFound() throws SQLException {
    // Arrange
    wireConnection();
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    // Act
    final Optional<AccountModel> result = repository.getByUserId(USER_ID);

    // Assert
    assertTrue(result.isEmpty(), "debe devolver Optional.empty()");
  }

  @Test
  @DisplayName("getByUserId() envuelve el SQLException en AccountPersistenceException")
  void shouldWrapSqlException() throws SQLException {
    // Arrange
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

    // Act & Assert
    assertThrows(AccountPersistenceException.class, () -> repository.getByUserId(USER_ID));
  }
}
