package com.bank.exception;

/**
 * Runtime exception representing low-level database connectivity,
 * SQL failure, or transactional rollback issues.
 */
public class DatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

