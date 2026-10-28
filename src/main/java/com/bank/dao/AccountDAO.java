package com.bank.dao;

import com.bank.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Account entity persistence and balance queries.
 */
public interface AccountDAO {

    /**
     * Persists an Account (Savings or Current) into the database.
     */
    void save(Account account);

    /**
     * Persists an Account within an existing transactional Connection.
     */
    void save(Account account, Connection conn);

    /**
     * Retrieves an account by its unique account number.
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Retrieves all accounts associated with a specific customer ID.
     */
    List<Account> findByCustomerId(String customerId);

    /**
     * Retrieves all accounts in the bank.
     */
    List<Account> findAll();

    /**
     * Updates an account's balance within a transactional Connection (used during atomic transfers).
     */
    void updateBalance(String accountNumber, BigDecimal newBalance, Connection conn);

    /**
     * Updates account state and balance using standard auto-commit connection.
     */
    void update(Account account);

    /**
     * Deletes an account by account number.
     */
    void delete(String accountNumber);
}

