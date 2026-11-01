package com.bank.model;

import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class representing a bank account.
 * Demonstrates Abstraction, Encapsulation, and Polymorphism.
 */
public abstract class Account implements Transactable, Auditable {

    // Private fields to guarantee strict Encapsulation
    private final String accountNumber;
    private final String customerId;
    private String accountHolderName;
    private BigDecimal balance;
    private final AccountType accountType;
    private AccountStatus status;
    private final LocalDateTime createdAt;

    /**
     * Parameterized Constructor for new account creation.
     */
    public Account(String accountNumber, String customerId, String accountHolderName,
                   BigDecimal initialBalance, AccountType accountType) {
        this(accountNumber, customerId, accountHolderName, initialBalance, accountType,
                AccountStatus.ACTIVE, LocalDateTime.now());
    }

    /**
     * Fully-parameterized Constructor used when hydrating account entity from DB.
     */
    public Account(String accountNumber, String customerId, String accountHolderName,
                   BigDecimal initialBalance, AccountType accountType,
                   AccountStatus status, LocalDateTime createdAt) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or blank.");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank.");
        }
        if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account holder name cannot be null or blank.");
        }
        if (initialBalance == null) {
            throw new IllegalArgumentException("Initial balance cannot be null.");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("Account type must be specified.");
        }

        this.accountNumber = accountNumber.trim();
        this.customerId = customerId.trim();
        this.accountHolderName = accountHolderName.trim();
        this.balance = initialBalance.setScale(2, RoundingMode.HALF_UP);
        this.accountType = accountType;
        this.status = (status != null) ? status : AccountStatus.ACTIVE;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    // ==================== Encapsulation (Getters & Setters) ====================

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account holder name cannot be null or blank.");
        }
        this.accountHolderName = accountHolderName.trim();
    }

    @Override
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Protected mutator allowing controlled balance adjustment by subclasses and DAO hydration.
     */
    protected void setBalance(BigDecimal balance) {
        if (balance == null) {
            throw new IllegalArgumentException("Balance cannot be set to null.");
        }
        this.balance = balance.setScale(2, RoundingMode.HALF_UP);
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Account status cannot be null.");
        }
        this.status = status;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ==================== Abstract Methods (Abstraction) ====================

    /**
     * Calculates interest accrued based on account-specific formulas and rates.
     * Subclasses must provide polymorphic implementation.
     *
     * @return computed interest amount 
     */
    public abstract BigDecimal calculateInterest();

    /**
     * Formats and returns a detailed display view of the account attributes and constraints.
     * Subclasses must provide polymorphic implementation.
     */
    public abstract String displayAccountDetails();

    /**
     * Polymorphic debit logic tailored for account-specific overdraft or minimum-balance constraints.
     */
    @Override
    public abstract void withdraw(BigDecimal amount) throws InsufficientBalanceException, InvalidTransactionException;

    // ==================== Polymorphism: Method Overloading ====================

    /**
     * Standard deposit method.
     */
    @Override
    public void deposit(BigDecimal amount) throws InvalidTransactionException {
        deposit(amount, "Standard Deposit");
    }

    /**
     * Overloaded deposit method accepting an audit note/narration.
     */
    public void deposit(BigDecimal amount, String narration) throws InvalidTransactionException {
        if (this.status != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(String.format("Cannot deposit to account %s in status %s.",
                    accountNumber, status));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be strictly greater than zero.");
        }

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        this.balance = this.balance.add(scaledAmount);
    }

    /**
     * Overloaded withdrawal method accepting an audit note/narration.
     */
    public void withdraw(BigDecimal amount, String narration) throws InsufficientBalanceException, InvalidTransactionException {
        // Delegates to polymorphic withdraw implementation
        withdraw(amount);
    }

    // ==================== Auditable Implementation ====================

    @Override
    public String getAuditId() {
        return this.accountNumber;
    }

    @Override
    public String getAuditSummary() {
        return String.format("[%s] AccNo: %s | Holder: %s | Bal: Rs. %s | Status: %s",
                accountType, accountNumber, accountHolderName, balance, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return displayAccountDetails();
    }
}

