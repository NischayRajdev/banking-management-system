package com.bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable ledger entry recording a banking transaction.
 * Demonstrates Auditable compliance, encapsulation, and domain integrity.
 */
public class Transaction implements Auditable {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String transactionId;
    private final String accountNumber;
    private final String relatedAccountNumber; // Populated for fund transfers
    private final LocalDateTime timestamp;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final TransactionStatus status;
    private final String remarks;

    /**
     * Parameterized Constructor for newly initiated transactions.
     */
    public Transaction(String accountNumber, String relatedAccountNumber,
                       TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, TransactionStatus status, String remarks) {
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                accountNumber, relatedAccountNumber, LocalDateTime.now(),
                type, amount, balanceAfter, status, remarks);
    }

    /**
     * Fully parameterized constructor for DB hydration.
     */
    public Transaction(String transactionId, String accountNumber, String relatedAccountNumber,
                       LocalDateTime timestamp, TransactionType type, BigDecimal amount,
                       BigDecimal balanceAfter, TransactionStatus status, String remarks) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or blank.");
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null.");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null.");
        }

        this.transactionId = transactionId.trim();
        this.accountNumber = accountNumber.trim();
        this.relatedAccountNumber = (relatedAccountNumber != null) ? relatedAccountNumber.trim() : null;
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
        this.type = type;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.balanceAfter = (balanceAfter != null) ? balanceAfter.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.status = (status != null) ? status : TransactionStatus.SUCCESS;
        this.remarks = (remarks != null) ? remarks.trim() : "";
    }

    // ==================== Encapsulation: Getters ====================

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getRelatedAccountNumber() {
        return relatedAccountNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    // ==================== Auditable Implementation ====================

    @Override
    public String getAuditId() {
        return this.transactionId;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return this.timestamp;
    }

    @Override
    public String getAuditSummary() {
        return String.format("[TXN-%s] %s | %s | Amount: Rs. %s | Bal After: Rs. %s | Status: %s | %s",
                transactionId, timestamp.format(TS_FORMAT), type, amount, balanceAfter, status, remarks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return getAuditSummary();
    }
}

