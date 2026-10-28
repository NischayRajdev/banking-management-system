package com.bank.dao;

import com.bank.exception.DatabaseException;
import com.bank.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of AccountDAO.
 * Handles polymorphic persistence and hydration of SavingsAccount and CurrentAccount
 * using PreparedStatements to thwart SQL injection vulnerabilities.
 */
public class AccountDAOImpl implements AccountDAO {

    @Override
    public void save(Account account) {
        try (Connection conn = DBConnection.getConnection()) {
            save(account, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to obtain connection for saving account: " + account.getAccountNumber(), e);
        }
    }

    @Override
    public void save(Account account, Connection conn) {
        String sql = """
                INSERT INTO accounts (
                    account_number, customer_id, account_holder_name, balance,
                    account_type, status, interest_rate, minimum_balance,
                    max_withdrawal_limit, overdraft_limit, overdraft_interest_rate, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getCustomerId());
            ps.setString(3, account.getAccountHolderName());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getAccountType().name());
            ps.setString(6, account.getStatus().name());

            if (account instanceof SavingsAccount savings) {
                ps.setBigDecimal(7, savings.getInterestRate());
                ps.setBigDecimal(8, savings.getMinimumBalance());
                ps.setBigDecimal(9, savings.getMaxWithdrawalPerTransaction());
                ps.setNull(10, Types.DECIMAL);
                ps.setNull(11, Types.DECIMAL);
            } else if (account instanceof CurrentAccount current) {
                ps.setNull(7, Types.DECIMAL);
                ps.setNull(8, Types.DECIMAL);
                ps.setNull(9, Types.DECIMAL);
                ps.setBigDecimal(10, current.getOverdraftLimit());
                ps.setBigDecimal(11, current.getOverdraftInterestRate());
            } else {
                ps.setNull(7, Types.DECIMAL);
                ps.setNull(8, Types.DECIMAL);
                ps.setNull(9, Types.DECIMAL);
                ps.setNull(10, Types.DECIMAL);
                ps.setNull(11, Types.DECIMAL);
            }

            ps.setString(12, account.getCreatedAt().toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist account: " + account.getAccountNumber(), e);
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error finding account: " + accountNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Account> findByCustomerId(String customerId) {
        String sql = "SELECT * FROM accounts WHERE customer_id = ? ORDER BY created_at ASC;";
        List<Account> accounts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customerId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapResultSetToAccount(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error finding accounts for customer: " + customerId, e);
        }
        return accounts;
    }

    @Override
    public List<Account> findAll() {
        String sql = "SELECT * FROM accounts ORDER BY created_at DESC;";
        List<Account> accounts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapResultSetToAccount(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all accounts.", e);
        }
        return accounts;
    }

    @Override
    public void updateBalance(String accountNumber, BigDecimal newBalance, Connection conn) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?;";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, accountNumber.trim());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("No account found to update balance for: " + accountNumber);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update balance for account: " + accountNumber, e);
        }
    }

    @Override
    public void update(Account account) {
        String sql = """
                UPDATE accounts
                SET account_holder_name = ?, balance = ?, status = ?,
                    interest_rate = ?, minimum_balance = ?, max_withdrawal_limit = ?,
                    overdraft_limit = ?, overdraft_interest_rate = ?
                WHERE account_number = ?;
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, account.getAccountHolderName());
            ps.setBigDecimal(2, account.getBalance());
            ps.setString(3, account.getStatus().name());

            if (account instanceof SavingsAccount savings) {
                ps.setBigDecimal(4, savings.getInterestRate());
                ps.setBigDecimal(5, savings.getMinimumBalance());
                ps.setBigDecimal(6, savings.getMaxWithdrawalPerTransaction());
                ps.setNull(7, Types.DECIMAL);
                ps.setNull(8, Types.DECIMAL);
            } else if (account instanceof CurrentAccount current) {
                ps.setNull(4, Types.DECIMAL);
                ps.setNull(5, Types.DECIMAL);
                ps.setNull(6, Types.DECIMAL);
                ps.setBigDecimal(7, current.getOverdraftLimit());
                ps.setBigDecimal(8, current.getOverdraftInterestRate());
            } else {
                ps.setNull(4, Types.DECIMAL);
                ps.setNull(5, Types.DECIMAL);
                ps.setNull(6, Types.DECIMAL);
                ps.setNull(7, Types.DECIMAL);
                ps.setNull(8, Types.DECIMAL);
            }

            ps.setString(9, account.getAccountNumber());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update account: " + account.getAccountNumber(), e);
        }
    }

    @Override
    public void delete(String accountNumber) {
        String sql = "DELETE FROM accounts WHERE account_number = ?;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber.trim());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete account: " + accountNumber, e);
        }
    }

    /**
     * Factory Method to reconstruct the polymorphic Account hierarchy
     * (SavingsAccount vs CurrentAccount) based on the persisted account_type.
     */
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        String accountNumber = rs.getString("account_number");
        String customerId = rs.getString("customer_id");
        String accountHolderName = rs.getString("account_holder_name");
        BigDecimal balance = rs.getBigDecimal("balance");
        String typeStr = rs.getString("account_type");
        String statusStr = rs.getString("status");
        String createdAtStr = rs.getString("created_at");

        AccountType type = AccountType.valueOf(typeStr);
        AccountStatus status = AccountStatus.valueOf(statusStr);
        LocalDateTime createdAt = DBConnection.parseDateTime(createdAtStr);

        if (type == AccountType.SAVINGS) {
            BigDecimal interestRate = rs.getBigDecimal("interest_rate");
            BigDecimal minBalance = rs.getBigDecimal("minimum_balance");
            BigDecimal maxWithdrawal = rs.getBigDecimal("max_withdrawal_limit");
            return new SavingsAccount(accountNumber, customerId, accountHolderName, balance, status, createdAt,
                    interestRate, minBalance, maxWithdrawal);
        } else if (type == AccountType.CURRENT) {
            BigDecimal overdraftLimit = rs.getBigDecimal("overdraft_limit");
            BigDecimal overdraftRate = rs.getBigDecimal("overdraft_interest_rate");
            return new CurrentAccount(accountNumber, customerId, accountHolderName, balance, status, createdAt,
                    overdraftLimit, overdraftRate);
        }

        throw new DatabaseException("Unsupported account type in database: " + typeStr);
    }
}

