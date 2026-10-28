package com.bank.model;

/**
 * Enumeration of financial and ledger transaction types.
 */
public enum TransactionType {
    DEPOSIT("Credit"),
    WITHDRAWAL("Debit"),
    TRANSFER_IN("Transfer Credit"),
    TRANSFER_OUT("Transfer Debit"),
    INTEREST_CREDIT("Interest Credit"),
    FEE_DEBIT("Service / Overdraft Fee");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

