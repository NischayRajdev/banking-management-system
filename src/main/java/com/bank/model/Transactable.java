package com.bank.model;

import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.InvalidTransactionException;

import java.math.BigDecimal;

/**
 * Interface defining transactional operations on financial accounts.
 */
public interface Transactable {

    /**
     * Credit funds to the account.
     *
     * @param amount the non-negative monetary value to deposit
     * @throws InvalidTransactionException if amount is null or <= 0
     */
    void deposit(BigDecimal amount) throws InvalidTransactionException;

    /**
     * Debit funds from the account according to its balance/overdraft rules.
     *
     * @param amount the non-negative monetary value to withdraw
     * @throws InsufficientBalanceException if account has inadequate balance or exceeds overdraft
     * @throws InvalidTransactionException if amount is invalid or violates constraints
     */
    void withdraw(BigDecimal amount) throws InsufficientBalanceException, InvalidTransactionException;

    /**
     * Get the current ledger balance of the account.
     */
    BigDecimal getBalance();
}

