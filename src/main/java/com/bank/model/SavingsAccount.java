package com.bank.model;

import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Concrete class representing a Savings Account.
 * Implements interest accumulation, minimum balance enforcement,
 * and maximum withdrawal thresholds.
 */
public class SavingsAccount extends Account {

    public static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.045"); // 4.5% annual
    public static final BigDecimal DEFAULT_MINIMUM_BALANCE = new BigDecimal("500.00");
    public static final BigDecimal DEFAULT_MAX_WITHDRAWAL_LIMIT = new BigDecimal("25000.00");

    private BigDecimal interestRate;
    private BigDecimal minimumBalance;
    private BigDecimal maxWithdrawalPerTransaction;

    /**
     * Constructor with default interest rate, minimum balance, and withdrawal limit.
     */
    public SavingsAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal initialBalance) {
        this(accountNumber, customerId, accountHolderName, initialBalance,
                DEFAULT_INTEREST_RATE, DEFAULT_MINIMUM_BALANCE, DEFAULT_MAX_WITHDRAWAL_LIMIT);
    }

    /**
     * Parameterized Constructor with custom rate and limits.
     */
    public SavingsAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal initialBalance, BigDecimal interestRate,
                          BigDecimal minimumBalance, BigDecimal maxWithdrawalPerTransaction) {
        super(accountNumber, customerId, accountHolderName, initialBalance, AccountType.SAVINGS);

        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative.");
        }
        if (minimumBalance == null || minimumBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum balance cannot be negative.");
        }
        if (initialBalance.compareTo(minimumBalance) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Initial deposit (%s) must be at least the required minimum balance (%s).",
                    initialBalance, minimumBalance));
        }
        if (maxWithdrawalPerTransaction == null || maxWithdrawalPerTransaction.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max withdrawal limit must be greater than zero.");
        }

        this.interestRate = interestRate;
        this.minimumBalance = minimumBalance.setScale(2, RoundingMode.HALF_UP);
        this.maxWithdrawalPerTransaction = maxWithdrawalPerTransaction.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Fully parameterized constructor for DB hydration.
     */
    public SavingsAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal balance, AccountStatus status, LocalDateTime createdAt,
                          BigDecimal interestRate, BigDecimal minimumBalance, BigDecimal maxWithdrawalPerTransaction) {
        super(accountNumber, customerId, accountHolderName, balance, AccountType.SAVINGS, status, createdAt);
        this.interestRate = (interestRate != null) ? interestRate : DEFAULT_INTEREST_RATE;
        this.minimumBalance = (minimumBalance != null) ? minimumBalance.setScale(2, RoundingMode.HALF_UP) : DEFAULT_MINIMUM_BALANCE;
        this.maxWithdrawalPerTransaction = (maxWithdrawalPerTransaction != null) ? maxWithdrawalPerTransaction.setScale(2, RoundingMode.HALF_UP) : DEFAULT_MAX_WITHDRAWAL_LIMIT;
    }

    // ==================== Encapsulation: Getters and Setters ====================

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative.");
        }
        this.interestRate = interestRate;
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(BigDecimal minimumBalance) {
        if (minimumBalance == null || minimumBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum balance cannot be negative.");
        }
        this.minimumBalance = minimumBalance.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMaxWithdrawalPerTransaction() {
        return maxWithdrawalPerTransaction;
    }

    public void setMaxWithdrawalPerTransaction(BigDecimal maxWithdrawalPerTransaction) {
        if (maxWithdrawalPerTransaction == null || maxWithdrawalPerTransaction.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max withdrawal limit must be greater than zero.");
        }
        this.maxWithdrawalPerTransaction = maxWithdrawalPerTransaction.setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== Polymorphism: Method Overriding ====================

    /**
     * Calculates annual interest accrued on current savings balance:
     * Interest = Balance * InterestRate
     */
    @Override
    public BigDecimal calculateInterest() {
        return getBalance()
                .multiply(interestRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Withdraws funds while enforcing:
     * 1. Account status is ACTIVE
     * 2. Per-transaction withdrawal ceiling
     * 3. Retention of mandatory minimum balance
     */
    @Override
    public void withdraw(BigDecimal amount) throws InsufficientBalanceException, InvalidTransactionException {
        if (getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(String.format("Cannot withdraw: Account %s is %s.",
                    getAccountNumber(), getStatus()));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be strictly positive.");
        }

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);

        if (scaledAmount.compareTo(maxWithdrawalPerTransaction) > 0) {
            throw new InvalidTransactionException(String.format(
                    "Withdrawal request of Rs. %s exceeds maximum per-transaction limit of Rs. %s for Savings Account %s.",
                    scaledAmount, maxWithdrawalPerTransaction, getAccountNumber()));
        }

        BigDecimal balanceAfterDebit = getBalance().subtract(scaledAmount);
        if (balanceAfterDebit.compareTo(minimumBalance) < 0) {
            throw new InsufficientBalanceException(String.format(
                    "Withdrawal of Rs. %s denied for account %s. Resulting balance (Rs. %s) would violate mandatory minimum balance (Rs. %s). Available to withdraw: Rs. %s",
                    scaledAmount, getAccountNumber(), balanceAfterDebit, minimumBalance,
                    getBalance().subtract(minimumBalance).max(BigDecimal.ZERO)));
        }

        setBalance(balanceAfterDebit);
    }

    @Override
    public String displayAccountDetails() {
        return String.format(
                "--- Savings Account Details ---\n" +
                "Account Number       : %s\n" +
                "Customer ID          : %s\n" +
                "Account Holder       : %s\n" +
                "Current Balance      : Rs. %s\n" +
                "Interest Rate        : %s%%\n" +
                "Minimum Balance      : Rs. %s\n" +
                "Max Withdrawal/Txn   : Rs. %s\n" +
                "Account Status       : %s\n" +
                "Created At           : %s",
                getAccountNumber(),
                getCustomerId(),
                getAccountHolderName(),
                getBalance(),
                interestRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                minimumBalance,
                maxWithdrawalPerTransaction,
                getStatus(),
                getCreatedAt()
        );
    }
}

