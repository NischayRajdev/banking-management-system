package com.bank.exception;

import java.math.BigDecimal;

/**
 * Thrown when a withdrawal or debit exceeds the sanctioned overdraft limit of a Current Account.
 */
public class OverdraftExceededException extends InsufficientBalanceException {

    private static final long serialVersionUID = 1L;
    private final BigDecimal overdraftLimit;
    private final BigDecimal totalAvailableLiquidity;

    public OverdraftExceededException(String accountNumber, BigDecimal currentBalance, BigDecimal overdraftLimit, BigDecimal requestedAmount) {
        super(String.format("Account %s exceeded overdraft limit! Balance: %s, Overdraft Limit: %s (Total Available: %s), Requested: %s",
                accountNumber, currentBalance, overdraftLimit, currentBalance.add(overdraftLimit), requestedAmount));
        this.overdraftLimit = overdraftLimit;
        this.totalAvailableLiquidity = currentBalance.add(overdraftLimit);
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public BigDecimal getTotalAvailableLiquidity() {
        return totalAvailableLiquidity;
    }
}

