package com.bank.exception;

/**
 * Base checked exception for all business and operational failures
 * in the Banking Management System.
 */
public class BankingException extends Exception {

    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public BankingException(String message) {
        super(message);
        this.errorCode = "BANK_GENERAL_ERR";
    }

    public BankingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BankingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", errorCode, getMessage());
    }
}

