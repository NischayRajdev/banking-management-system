package com.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Entity representing a bank customer.
 * Manages customer demographic details and associated bank accounts collection.
 */
public class Customer implements Auditable {

    private final String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private final LocalDateTime createdAt;

    // Encapsulated collection of associated bank accounts (Key: Account Number, Value: Account)
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    /**
     * Parameterized Constructor for registering a new customer.
     */
    public Customer(String customerId, String firstName, String lastName,
                    String email, String phoneNumber, String address) {
        this(customerId, firstName, lastName, email, phoneNumber, address, LocalDateTime.now());
    }

    /**
     * Fully parameterized constructor for DB hydration.
     */
    public Customer(String customerId, String firstName, String lastName,
                    String email, String phoneNumber, String address, LocalDateTime createdAt) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank.");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or blank.");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or blank.");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address must be provided.");
        }

        this.customerId = customerId.trim();
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.email = email.trim().toLowerCase();
        this.phoneNumber = (phoneNumber != null) ? phoneNumber.trim() : "";
        this.address = (address != null) ? address.trim() : "";
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    // ==================== Encapsulation: Getters and Setters ====================

    public String getCustomerId() {
        return customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty.");
        }
        this.firstName = firstName.trim();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty.");
        }
        this.lastName = lastName.trim();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email must be provided.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = (phoneNumber != null) ? phoneNumber.trim() : "";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = (address != null) ? address.trim() : "";
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ==================== Account Collection Management ====================

    /**
     * Associates a new account with this customer.
     */
    public void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Cannot attach null account to customer.");
        }
        accounts.put(account.getAccountNumber(), account);
    }

    /**
     * Removes an associated account.
     */
    public boolean removeAccount(String accountNumber) {
        return accounts.remove(accountNumber) != null;
    }

    /**
     * Retrieves an account by its unique account number.
     */
    public Optional<Account> getAccount(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    /**
     * Returns an unmodifiable list of all accounts associated with this customer.
     * Prevents external code from mutating internal state without using proper methods.
     */
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accounts.values()));
    }

    /**
     * Returns a copy of the accounts map.
     */
    public Map<String, Account> getAccountsMap() {
        return Collections.unmodifiableMap(accounts);
    }

    /**
     * Calculates the aggregate balance across all accounts owned by this customer.
     */
    public BigDecimal getTotalPortfolioBalance() {
        return accounts.values().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== Auditable Implementation ====================

    @Override
    public String getAuditId() {
        return this.customerId;
    }

    @Override
    public String getAuditSummary() {
        return String.format("Customer [%s] %s | Email: %s | Accounts: %d | Portfolio Bal: Rs. %s",
                customerId, getFullName(), email, accounts.size(), getTotalPortfolioBalance());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return String.format("Customer[ID=%s, Name=%s, Email=%s, TotalAccounts=%d, NetWorth=Rs. %s]",
                customerId, getFullName(), email, accounts.size(), getTotalPortfolioBalance());
    }
}

