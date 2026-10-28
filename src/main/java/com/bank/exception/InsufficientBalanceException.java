package com.bank.exception;

import java.math.BigDecimal;

/**
 * Thrown when an account does not have sufficient balance or minimum balance
 * to satisfy a debit/withdrawal/transfer operation.
 */
public class InsufficientBalanceException extends BankingException {

    private static final long serialVersionUID = 1L;
    private final String accountNumber;
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(String accountNumber, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super("ERR_INSUFFICIENT_FUNDS",
                String.format("Account %s has insufficient funds. Current balance: %s, Requested debit: %s",
                        accountNumber, currentBalance, requestedAmount));
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public InsufficientBalanceException(String message) {
        super("ERR_INSUFFICIENT_FUNDS", message);
        this.accountNumber = null;
        this.currentBalance = null;
        this.requestedAmount = null;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}

