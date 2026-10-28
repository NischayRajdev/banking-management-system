package com.bank.dao;

import com.bank.model.Transaction;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for financial transaction ledger records.
 */
public interface TransactionDAO {

    /**
     * Persists a transaction record into the database.
     */
    void save(Transaction transaction);

    /**
     * Persists a transaction record within an ongoing transactional Connection.
     */
    void save(Transaction transaction, Connection conn);

    /**
     * Retrieves all ledger transactions for a specific account, sorted chronologically.
     */
    List<Transaction> findByAccountNumber(String accountNumber);

    /**
     * Retrieves all ledger transactions across the entire banking institution.
     */
    List<Transaction> findAll();

    /**
     * Finds a single transaction by ID.
     */
    Optional<Transaction> findById(String transactionId);
}

