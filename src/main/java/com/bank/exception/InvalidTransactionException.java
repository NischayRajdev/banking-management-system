package com.bank.exception;

/**
 * Thrown when an illegal or logically invalid banking transaction is attempted
 * (e.g. non-positive deposit/withdrawal, closed account, per-transaction limit exceeded).
 */
public class InvalidTransactionException extends BankingException {

    private static final long serialVersionUID = 1L;

    public InvalidTransactionException(String message) {
        super("ERR_INVALID_TRANSACTION", message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super("ERR_INVALID_TRANSACTION", message, cause);
    }
}

