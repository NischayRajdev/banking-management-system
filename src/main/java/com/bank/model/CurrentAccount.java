package com.bank.model;

import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.InvalidTransactionException;
import com.bank.exception.OverdraftExceededException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Concrete class representing a Current (Checking) Account.
 * Provides overdraft credit facilities and commercial fee structures.
 */
public class CurrentAccount extends Account {

    public static final BigDecimal DEFAULT_OVERDRAFT_LIMIT = new BigDecimal("5000.00");
    public static final BigDecimal DEFAULT_OVERDRAFT_INTEREST_RATE = new BigDecimal("0.12"); // 12% APR on overdraft

    private BigDecimal overdraftLimit;
    private BigDecimal overdraftInterestRate;

    /**
     * Standard Constructor with default overdraft parameters.
     */
    public CurrentAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal initialBalance) {
        this(accountNumber, customerId, accountHolderName, initialBalance,
                DEFAULT_OVERDRAFT_LIMIT, DEFAULT_OVERDRAFT_INTEREST_RATE);
    }

    /**
     * Parameterized Constructor with custom overdraft limit and fee rate.
     */
    public CurrentAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal initialBalance, BigDecimal overdraftLimit,
                          BigDecimal overdraftInterestRate) {
        super(accountNumber, customerId, accountHolderName, initialBalance, AccountType.CURRENT);

        if (overdraftLimit == null || overdraftLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        }
        if (overdraftInterestRate == null || overdraftInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Overdraft interest rate cannot be negative.");
        }

        this.overdraftLimit = overdraftLimit.setScale(2, RoundingMode.HALF_UP);
        this.overdraftInterestRate = overdraftInterestRate;
    }

    /**
     * Fully parameterized constructor for DB hydration.
     */
    public CurrentAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal balance, AccountStatus status, LocalDateTime createdAt,
                          BigDecimal overdraftLimit, BigDecimal overdraftInterestRate) {
        super(accountNumber, customerId, accountHolderName, balance, AccountType.CURRENT, status, createdAt);
        this.overdraftLimit = (overdraftLimit != null) ? overdraftLimit.setScale(2, RoundingMode.HALF_UP) : DEFAULT_OVERDRAFT_LIMIT;
        this.overdraftInterestRate = (overdraftInterestRate != null) ? overdraftInterestRate : DEFAULT_OVERDRAFT_INTEREST_RATE;
    }

    // ==================== Encapsulation: Getters and Setters ====================

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        if (overdraftLimit == null || overdraftLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        }
        this.overdraftLimit = overdraftLimit.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getOverdraftInterestRate() {
        return overdraftInterestRate;
    }

    public void setOverdraftInterestRate(BigDecimal overdraftInterestRate) {
        if (overdraftInterestRate == null || overdraftInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Overdraft interest rate cannot be negative.");
        }
        this.overdraftInterestRate = overdraftInterestRate;
    }

    /**
     * Calculates total liquid funds available for spending = Balance + Overdraft Limit.
     */
    public BigDecimal getTotalAvailableLiquidity() {
        return getBalance().add(overdraftLimit);
    }

    // ==================== Polymorphism: Method Overriding ====================

    /**
     * Overrides interest calculation:
     * In commercial banking, Current Accounts earn 0% interest on positive balances.
     * When overdrawn (negative balance), it calculates the overdraft interest charge owed.
     */
    @Override
    public BigDecimal calculateInterest() {
        if (getBalance().compareTo(BigDecimal.ZERO) < 0) {
            // Negative balance means borrower owes overdraft fee/interest:
            // Charge = abs(balance) * overdraftInterestRate
            return getBalance().abs()
                    .multiply(overdraftInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Overrides withdrawal logic:
     * Allows withdrawing beyond balance into negative territory, bounded strictly
     * by the sanctioned overdraft limit.
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
        BigDecimal projectedBalance = getBalance().subtract(scaledAmount);

        // Check if projected balance breaches negative overdraft limit:
        // i.e., projectedBalance < -overdraftLimit
        if (projectedBalance.compareTo(overdraftLimit.negate()) < 0) {
            throw new OverdraftExceededException(
                    getAccountNumber(),
                    getBalance(),
                    overdraftLimit,
                    scaledAmount
            );
        }

        setBalance(projectedBalance);
    }

    @Override
    public String displayAccountDetails() {
        BigDecimal overdrawnAmount = getBalance().compareTo(BigDecimal.ZERO) < 0
                ? getBalance().abs()
                : BigDecimal.ZERO;

        BigDecimal remainingOverdraft = overdraftLimit.subtract(overdrawnAmount);

        return String.format(
                "--- Current Account Details ---\n" +
                "Account Number       : %s\n" +
                "Customer ID          : %s\n" +
                "Account Holder       : %s\n" +
                "Current Balance      : Rs. %s\n" +
                "Overdraft Limit      : Rs. %s\n" +
                "Utilized Overdraft   : Rs. %s\n" +
                "Available Overdraft  : Rs. %s\n" +
                "Total Liquidity      : Rs. %s\n" +
                "Overdraft Int. Rate  : %s%%\n" +
                "Account Status       : %s\n" +
                "Created At           : %s",
                getAccountNumber(),
                getCustomerId(),
                getAccountHolderName(),
                getBalance(),
                overdraftLimit,
                overdrawnAmount,
                remainingOverdraft,
                getTotalAvailableLiquidity(),
                overdraftInterestRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP),
                getStatus(),
                getCreatedAt()
        );
    }
}

