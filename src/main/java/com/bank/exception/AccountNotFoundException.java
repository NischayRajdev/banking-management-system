package com.bank.exception;

/**
 * Thrown when an account lookup fails by account number.
 */
public class AccountNotFoundException extends BankingException {

    private static final long serialVersionUID = 1L;
    private final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super("ERR_ACCOUNT_NOT_FOUND", String.format("Account with number '%s' was not found.", accountNumber));
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}

