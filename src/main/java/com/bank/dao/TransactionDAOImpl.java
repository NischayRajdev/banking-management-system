package com.bank.dao;

import com.bank.exception.DatabaseException;
import com.bank.model.Transaction;
import com.bank.model.TransactionStatus;
import com.bank.model.TransactionType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of TransactionDAO using PreparedStatement queries.
 */
public class TransactionDAOImpl implements TransactionDAO {

    @Override
    public void save(Transaction transaction) {
        try (Connection conn = DBConnection.getConnection()) {
            save(transaction, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to acquire connection for saving transaction: " + transaction.getTransactionId(), e);
        }
    }

    @Override
    public void save(Transaction transaction, Connection conn) {
        String sql = """
                INSERT INTO transactions (
                    transaction_id, account_number, related_account_number,
                    type, amount, balance_after, status, remarks, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transaction.getTransactionId());
            ps.setString(2, transaction.getAccountNumber());

            if (transaction.getRelatedAccountNumber() != null) {
                ps.setString(3, transaction.getRelatedAccountNumber());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.setString(4, transaction.getType().name());
            ps.setBigDecimal(5, transaction.getAmount());
            ps.setBigDecimal(6, transaction.getBalanceAfter());
            ps.setString(7, transaction.getStatus().name());
            ps.setString(8, transaction.getRemarks());
            ps.setString(9, transaction.getTimestamp().toString());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to persist transaction ledger: " + transaction.getTransactionId(), e);
        }
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM transactions WHERE account_number = ? ORDER BY timestamp ASC;";
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, accountNumber.trim());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving transactions for account: " + accountNumber, e);
        }
        return transactions;
    }

    @Override
    public List<Transaction> findAll() {
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC;";
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all transaction records.", e);
        }
        return transactions;
    }

    @Override
    public Optional<Transaction> findById(String transactionId) {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, transactionId.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTransaction(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error finding transaction by ID: " + transactionId, e);
        }
        return Optional.empty();
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        String id = rs.getString("transaction_id");
        String accNo = rs.getString("account_number");
        String relAccNo = rs.getString("related_account_number");
        String typeStr = rs.getString("type");
        BigDecimal amount = rs.getBigDecimal("amount");
        BigDecimal balAfter = rs.getBigDecimal("balance_after");
        String statusStr = rs.getString("status");
        String remarks = rs.getString("remarks");
        String tsStr = rs.getString("timestamp");

        LocalDateTime ts = DBConnection.parseDateTime(tsStr);
        TransactionType type = TransactionType.valueOf(typeStr);
        TransactionStatus status = TransactionStatus.valueOf(statusStr);

        return new Transaction(id, accNo, relAccNo, ts, type, amount, balAfter, status, remarks);
    }
}

