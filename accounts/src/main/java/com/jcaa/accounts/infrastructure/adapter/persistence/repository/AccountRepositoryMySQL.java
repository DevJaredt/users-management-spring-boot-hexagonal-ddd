package com.jcaa.accounts.infrastructure.adapter.persistence.repository;

import com.jcaa.accounts.application.port.out.GetAccountByUserIdPort;
import com.jcaa.accounts.application.port.out.SaveAccountPort;
import com.jcaa.accounts.application.port.out.UpdateAccountPort;
import com.jcaa.accounts.domain.exception.AccountNotFoundException;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.infrastructure.adapter.persistence.exception.AccountPersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Persistencia de cuentas sobre MySQL/TiDB (paso 6 del diagrama).
 *
 * <p>{@link #save(AccountModel)} es idempotente: si el servicio users reintenta la llamada, se
 * devuelve la cuenta existente en lugar de duplicarla.
 */
@Repository
@RequiredArgsConstructor
public class AccountRepositoryMySQL
    implements SaveAccountPort, GetAccountByUserIdPort, UpdateAccountPort {

  private static final String SQL_SELECT_BY_USER_ID =
      "SELECT id, user_id, name, email FROM accounts WHERE user_id = ? LIMIT 1";

  private static final String SQL_INSERT =
      "INSERT INTO accounts (id, user_id, name, email, created_at, updated_at) "
      + "VALUES (?, ?, ?, ?, NOW(), NOW())";

  private static final String SQL_UPDATE_BY_USER_ID =
      "UPDATE accounts SET name = ?, email = ?, updated_at = NOW() WHERE user_id = ?";

  private static final String ERROR_INSERT = "No se pudo crear la cuenta.";
  private static final String ERROR_SELECT = "No se pudo consultar la cuenta.";
  private static final String ERROR_UPDATE = "No se pudo actualizar la cuenta.";

  private final DataSource dataSource;

  @Override
  public AccountModel save(final AccountModel account) {
    return getByUserId(account.userId()).orElseGet(() -> insertAndReload(account));
  }

  @Override
  public Optional<AccountModel> getByUserId(final String userId) {
    try (final Connection connection = dataSource.getConnection();
         final PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_USER_ID)) {
      statement.setString(1, userId);
      final ResultSet resultSet = statement.executeQuery();
      if (!resultSet.next()) {
        return Optional.empty();
      }
      return Optional.of(mapRow(resultSet));
    } catch (final SQLException exception) {
      throw new AccountPersistenceException(ERROR_SELECT, exception);
    }
  }

  private AccountModel insertAndReload(final AccountModel account) {
    try (final Connection connection = dataSource.getConnection();
         final PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {
      statement.setString(1, account.id());
      statement.setString(2, account.userId());
      statement.setString(3, account.name());
      statement.setString(4, account.email());
      statement.executeUpdate();
    } catch (final SQLException exception) {
      throw new AccountPersistenceException(ERROR_INSERT, exception);
    }
    return getByUserId(account.userId()).orElse(account);
  }

  @Override
  public AccountModel updateByUserId(final String userId, final String name, final String email) {
    try (final Connection connection = dataSource.getConnection();
         final PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_BY_USER_ID)) {
      statement.setString(1, name);
      statement.setString(2, email);
      statement.setString(3, userId);
      statement.executeUpdate();
    } catch (final SQLException exception) {
      throw new AccountPersistenceException(ERROR_UPDATE, exception);
    }
    return getByUserId(userId)
        .orElseThrow(() -> AccountNotFoundException.becauseUserIdWasNotFound(userId));
  }

  private static AccountModel mapRow(final ResultSet resultSet) throws SQLException {
    return new AccountModel(
        resultSet.getString("id"),
        resultSet.getString("user_id"),
        resultSet.getString("name"),
        resultSet.getString("email"));
  }
}
