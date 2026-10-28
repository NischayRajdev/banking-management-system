package com.bank.service;

import com.bank.dao.*;
import com.bank.exception.*;
import com.bank.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Central Service Controller for the Banking Management System.
 * Manages collections of customers and accounts, coordinates ACID database
 * persistence, enforces business rules, and exposes financial workflows.
 */
public class Bank implements BankService {

    private final String bankName;
    private final String branchCode;

    // In-memory collections managing domain state
    private final Map<String, Customer> customerRegistry = new ConcurrentHashMap<>();
    private final Map<String, Account> accountRegistry = new ConcurrentHashMap<>();

    // Persistence Layer (DAOs)
    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    // Monotonic ID sequences
    private final AtomicLong customerIdSequence = new AtomicLong(1000);
    private final AtomicLong savingsAccountSequence = new AtomicLong(5000);
    private final AtomicLong currentAccountSequence = new AtomicLong(8000);

    /**
     * Default Constructor initializing with production DAOs and auto-loading existing state.
     */
    public Bank() {
        this("India Bank", "INDB-001",
                new CustomerDAOImpl(), new AccountDAOImpl(), new TransactionDAOImpl());
    }

    /**
     * Dependency Injection Constructor enabling custom DAO mocks or configurations.
     */
    public Bank(String bankName, String branchCode,
                CustomerDAO customerDAO, AccountDAO accountDAO, TransactionDAO transactionDAO) {
        if (bankName == null || bankName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank name cannot be null or blank.");
        }
        if (branchCode == null || branchCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch code cannot be null or blank.");
        }

        this.bankName = bankName.trim();
        this.branchCode = branchCode.trim();
        this.customerDAO = Objects.requireNonNull(customerDAO, "CustomerDAO cannot be null.");
        this.accountDAO = Objects.requireNonNull(accountDAO, "AccountDAO cannot be null.");
        this.transactionDAO = Objects.requireNonNull(transactionDAO, "TransactionDAO cannot be null.");

        // Synchronize in-memory collections with database state
        reloadFromDatabase();
    }

    // ==================== Encapsulation: Getters ====================

    public String getBankName() {
        return bankName;
    }

    public String getBranchCode() {
        return branchCode;
    }

    // ==================== Synchronization with Database ====================

    /**
     * Hydrates the in-memory registry collections from the relational database.
     * Synchronizes sequence counters with highest persisted primary keys.
     */
    public final synchronized void reloadFromDatabase() {
        customerRegistry.clear();
        accountRegistry.clear();

        List<Customer> persistedCustomers = customerDAO.findAll();
        long maxCustId = 1000;
        for (Customer customer : persistedCustomers) {
            customerRegistry.put(customer.getCustomerId(), customer);
            try {
                String id = customer.getCustomerId();
                String digits = id.replaceAll("\\D+", "");
                if (!digits.isEmpty()) {
                    long val = Long.parseLong(digits);
                    if (val > maxCustId) {
                        maxCustId = val;
                    }
                }
            } catch (Exception ignored) {}
        }
        customerIdSequence.set(maxCustId);

        List<Account> persistedAccounts = accountDAO.findAll();
        long maxSav = 5000;
        long maxCur = 8000;
        for (Account account : persistedAccounts) {
            accountRegistry.put(account.getAccountNumber(), account);

            // Re-link account with its customer aggregate
            Customer owner = customerRegistry.get(account.getCustomerId());
            if (owner != null) {
                owner.addAccount(account);
            }

            try {
                String accNum = account.getAccountNumber();
                String digits = accNum.replaceAll("\\D+", "");
                if (!digits.isEmpty()) {
                    long val = Long.parseLong(digits);
                    if (account instanceof SavingsAccount && val > maxSav) {
                        maxSav = val;
                    } else if (account instanceof CurrentAccount && val > maxCur) {
                        maxCur = val;
                    }
                }
            } catch (Exception ignored) {}
        }
        savingsAccountSequence.set(maxSav);
        currentAccountSequence.set(maxCur);
    }

    /**
     * Resets the underlying database schema and refreshes in-memory state.
     */
    public synchronized void resetDatabaseAndReload() {
        DBConnection.resetDatabase();
        reloadFromDatabase();
    }

    // ==================== Customer Operations ====================

    @Override
    public synchronized Customer registerCustomer(String firstName, String lastName,
                                                   String email, String phone, String address) throws BankingException {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new BankingException("ERR_INVALID_INPUT", "Customer first name cannot be blank.");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new BankingException("ERR_INVALID_INPUT", "Customer last name cannot be blank.");
        }
        if (email == null || !email.contains("@")) {
            throw new BankingException("ERR_INVALID_INPUT", "A valid email address is mandatory.");
        }

        // Enforce uniqueness of email
        Optional<Customer> existing = customerDAO.findByEmail(email);
        if (existing.isPresent()) {
            throw new BankingException("ERR_DUPLICATE_CUSTOMER",
                    String.format("A customer with email '%s' already exists (Customer ID: %s).",
                            email, existing.get().getCustomerId()));
        }

        String customerId = "CUST-" + customerIdSequence.incrementAndGet();
        Customer customer = new Customer(customerId, firstName, lastName, email, phone, address);

        // Persist to relational DB
        customerDAO.save(customer);

        // Update in-memory registry
        customerRegistry.put(customerId, customer);
        return customer;
    }

    // ==================== Account Creation Operations ====================

    @Override
    public SavingsAccount openSavingsAccount(String customerId, BigDecimal initialDeposit) throws BankingException {
        return openSavingsAccount(customerId, initialDeposit,
                SavingsAccount.DEFAULT_INTEREST_RATE,
                SavingsAccount.DEFAULT_MINIMUM_BALANCE,
                SavingsAccount.DEFAULT_MAX_WITHDRAWAL_LIMIT);
    }

    @Override
    public synchronized SavingsAccount openSavingsAccount(String customerId, BigDecimal initialDeposit,
                                                          BigDecimal interestRate, BigDecimal minBalance,
                                                          BigDecimal maxWithdrawal) throws BankingException {
        Customer customer = getCustomerOrThrow(customerId);

        if (initialDeposit == null || initialDeposit.compareTo(minBalance) < 0) {
            throw new InvalidTransactionException(String.format(
                    "Initial deposit (Rs. %s) cannot be lower than the required minimum balance of Rs. %s.",
                    initialDeposit, minBalance));
        }

        String accountNumber = "SAV-" + branchCode + "-" + savingsAccountSequence.incrementAndGet();
        SavingsAccount savingsAccount = new SavingsAccount(
                accountNumber, customerId, customer.getFullName(), initialDeposit,
                interestRate, minBalance, maxWithdrawal
        );

        // Persist Account entity
        accountDAO.save(savingsAccount);

        // Record initial ledger transaction
        Transaction initialTxn = new Transaction(
                accountNumber, null, TransactionType.DEPOSIT,
                initialDeposit, initialDeposit, TransactionStatus.SUCCESS,
                "Initial Account Opening Deposit"
        );
        transactionDAO.save(initialTxn);

        // Update in-memory graphs
        customer.addAccount(savingsAccount);
        accountRegistry.put(accountNumber, savingsAccount);

        return savingsAccount;
    }

    @Override
    public CurrentAccount openCurrentAccount(String customerId, BigDecimal initialDeposit) throws BankingException {
        return openCurrentAccount(customerId, initialDeposit,
                CurrentAccount.DEFAULT_OVERDRAFT_LIMIT,
                CurrentAccount.DEFAULT_OVERDRAFT_INTEREST_RATE);
    }

    @Override
    public synchronized CurrentAccount openCurrentAccount(String customerId, BigDecimal initialDeposit,
                                                          BigDecimal overdraftLimit,
                                                          BigDecimal overdraftInterestRate) throws BankingException {
        Customer customer = getCustomerOrThrow(customerId);

        if (initialDeposit == null || initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Initial deposit cannot be negative.");
        }

        String accountNumber = "CUR-" + branchCode + "-" + currentAccountSequence.incrementAndGet();
        CurrentAccount currentAccount = new CurrentAccount(
                accountNumber, customerId, customer.getFullName(), initialDeposit,
                overdraftLimit, overdraftInterestRate
        );

        // Persist Account entity
        accountDAO.save(currentAccount);

        // Record initial ledger transaction if deposit > 0
        if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            Transaction initialTxn = new Transaction(
                    accountNumber, null, TransactionType.DEPOSIT,
                    initialDeposit, initialDeposit, TransactionStatus.SUCCESS,
                    "Initial Current Account Opening Deposit"
            );
            transactionDAO.save(initialTxn);
        }

        // Update in-memory graphs
        customer.addAccount(currentAccount);
        accountRegistry.put(accountNumber, currentAccount);

        return currentAccount;
    }

    // ==================== Transaction Operations: Method Overloading ====================

    @Override
    public Transaction deposit(String accountNumber, BigDecimal amount) throws BankingException {
        return deposit(accountNumber, amount, "Standard Deposit");
    }

    @Override
    public synchronized Transaction deposit(String accountNumber, BigDecimal amount, String remarks) throws BankingException {
        Account account = getAccountOrThrow(accountNumber);

        // Polymorphic credit with validation
        account.deposit(amount, remarks);

        // Synchronize with database
        accountDAO.update(account);

        // Record immutable transaction
        Transaction txn = new Transaction(
                accountNumber, null, TransactionType.DEPOSIT,
                amount, account.getBalance(), TransactionStatus.SUCCESS, remarks
        );
        transactionDAO.save(txn);

        return txn;
    }

    @Override
    public Transaction withdraw(String accountNumber, BigDecimal amount) throws BankingException {
        return withdraw(accountNumber, amount, "Standard Withdrawal");
    }

    @Override
    public synchronized Transaction withdraw(String accountNumber, BigDecimal amount, String remarks) throws BankingException {
        Account account = getAccountOrThrow(accountNumber);

        // Polymorphic debit (enforces Savings min-balance or Current overdraft limits)
        account.withdraw(amount, remarks);

        // Synchronize with database
        accountDAO.update(account);

        // Record immutable transaction
        Transaction txn = new Transaction(
                accountNumber, null, TransactionType.WITHDRAWAL,
                amount, account.getBalance(), TransactionStatus.SUCCESS, remarks
        );
        transactionDAO.save(txn);

        return txn;
    }

    @Override
    public List<Transaction> transfer(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount)
            throws BankingException {
        return transfer(sourceAccountNumber, destinationAccountNumber, amount, "Direct Account Transfer");
    }

    /**
     * Executes an ACID-compliant fund transfer between two accounts using JDBC transaction control.
     * Guarantees atomic commit or full rollback upon any failure.
     */
    @Override
    public synchronized List<Transaction> transfer(String sourceAccountNumber,
                                                   String destinationAccountNumber,
                                                   BigDecimal amount,
                                                   String remarks) throws BankingException {
        if (sourceAccountNumber == null || destinationAccountNumber == null) {
            throw new InvalidTransactionException("Source and destination account numbers must be non-null.");
        }
        if (sourceAccountNumber.trim().equalsIgnoreCase(destinationAccountNumber.trim())) {
            throw new InvalidTransactionException("Self-transfers to the same account number are prohibited.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be strictly greater than zero.");
        }

        Account source = getAccountOrThrow(sourceAccountNumber);
        Account destination = getAccountOrThrow(destinationAccountNumber);

        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Source account is not active: " + source.getStatus());
        }
        if (destination.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Destination account is not active: " + destination.getStatus());
        }

        // Validate withdrawal rules (dry-run check before database lock)
        BigDecimal sourceBalanceBefore = source.getBalance();
        BigDecimal destBalanceBefore = destination.getBalance();

        // Perform in-memory debit and credit test
        source.withdraw(amount);
        destination.deposit(amount);

        BigDecimal sourceBalanceAfter = source.getBalance();
        BigDecimal destBalanceAfter = destination.getBalance();

        // Revert in-memory values to maintain consistency until DB commit succeeds
        // We will finalize in-memory only upon DB commit
        Transaction sourceTxn = new Transaction(
                source.getAccountNumber(), destination.getAccountNumber(),
                TransactionType.TRANSFER_OUT, amount, sourceBalanceAfter,
                TransactionStatus.SUCCESS, remarks + " -> Sent to " + destination.getAccountNumber()
        );

        Transaction destTxn = new Transaction(
                destination.getAccountNumber(), source.getAccountNumber(),
                TransactionType.TRANSFER_IN, amount, destBalanceAfter,
                TransactionStatus.SUCCESS, remarks + " <- Received from " + source.getAccountNumber()
        );

        // Execute atomic JDBC Transaction
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin ACID transaction block

            accountDAO.updateBalance(source.getAccountNumber(), sourceBalanceAfter, conn);
            accountDAO.updateBalance(destination.getAccountNumber(), destBalanceAfter, conn);

            transactionDAO.save(sourceTxn, conn);
            transactionDAO.save(destTxn, conn);

            conn.commit(); // Atomic commit
            return List.of(sourceTxn, destTxn);

        } catch (SQLException | DatabaseException e) {
            // Rollback DB changes
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("CRITICAL: Failed to rollback transaction: " + rollbackEx.getMessage());
                }
            }
            // Revert in-memory changes
            source.deposit(amount); // restore source
            try {
                destination.withdraw(amount); // restore destination
            } catch (Exception ignored) {}

            throw new BankingException("ERR_TRANSFER_FAILED",
                    "Atomic fund transfer failed and was safely rolled back: " + e.getMessage(), e);

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    // ==================== Balance & Interest Operations ====================

    @Override
    public BigDecimal checkBalance(String accountNumber) throws AccountNotFoundException {
        return getAccountOrThrow(accountNumber).getBalance();
    }

    @Override
    public synchronized Transaction applyInterest(String accountNumber) throws BankingException {
        Account account = getAccountOrThrow(accountNumber);

        // Polymorphic interest calculation
        BigDecimal interestAmount = account.calculateInterest();

        if (interestAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException(
                    String.format("Calculated interest for account %s is Rs. %s; no interest accrued.",
                            accountNumber, interestAmount));
        }

        if (account instanceof SavingsAccount) {
            // For Savings Accounts, interest is credited to balance
            account.deposit(interestAmount, "Annual Interest Credit");
            accountDAO.update(account);

            Transaction interestTxn = new Transaction(
                    accountNumber, null, TransactionType.INTEREST_CREDIT,
                    interestAmount, account.getBalance(), TransactionStatus.SUCCESS,
                    "Annual Interest Accrual Credited"
            );
            transactionDAO.save(interestTxn);
            return interestTxn;

        } else if (account instanceof CurrentAccount && account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            // For overdrawn Current Accounts, overdraft interest fee is debited
            account.withdraw(interestAmount, "Overdraft Interest Charge");
            accountDAO.update(account);

            Transaction feeTxn = new Transaction(
                    accountNumber, null, TransactionType.FEE_DEBIT,
                    interestAmount, account.getBalance(), TransactionStatus.SUCCESS,
                    "Overdraft Facility Interest Charged"
            );
            transactionDAO.save(feeTxn);
            return feeTxn;
        }

        throw new InvalidTransactionException("Account type does not support interest posting in its current state.");
    }

    // ==================== Query Operations: Method Overloading ====================

    @Override
    public Account findAccount(String accountNumber) throws AccountNotFoundException {
        return getAccountOrThrow(accountNumber);
    }

    @Override
    public List<Account> findAccountsByCustomer(String customerId) throws CustomerNotFoundException {
        Customer customer = getCustomerOrThrow(customerId);
        return customer.getAccounts();
    }

    @Override
    public List<Account> findAccountsByBalanceRange(BigDecimal minBalance, BigDecimal maxBalance) {
        if (minBalance == null || maxBalance == null) {
            throw new IllegalArgumentException("Balance search bounds cannot be null.");
        }
        return accountRegistry.values().stream()
                .filter(acc -> acc.getBalance().compareTo(minBalance) >= 0 &&
                        acc.getBalance().compareTo(maxBalance) <= 0)
                .collect(Collectors.toList());
    }

    @Override
    public Customer findCustomer(String customerId) throws CustomerNotFoundException {
        return getCustomerOrThrow(customerId);
    }

    @Override
    public Customer findCustomerByEmail(String email) throws CustomerNotFoundException {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null.");
        }
        return customerRegistry.values().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst()
                .orElseThrow(() -> new CustomerNotFoundException(email));
    }

    @Override
    public List<Customer> getAllCustomers() {
        return Collections.unmodifiableList(new ArrayList<>(customerRegistry.values()));
    }

    @Override
    public List<Account> getAllAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accountRegistry.values()));
    }

    @Override
    public List<Transaction> getAccountStatement(String accountNumber) throws AccountNotFoundException {
        getAccountOrThrow(accountNumber); // Verify existence
        return transactionDAO.findByAccountNumber(accountNumber);
    }

    @Override
    public synchronized Customer updateCustomer(String customerId, String firstName, String lastName,
                                                String phone, String address) throws CustomerNotFoundException {
        Customer customer = getCustomerOrThrow(customerId);
        if (firstName != null && !firstName.trim().isEmpty()) {
            customer.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            customer.setLastName(lastName.trim());
        }
        if (phone != null) {
            customer.setPhoneNumber(phone.trim());
        }
        if (address != null) {
            customer.setAddress(address.trim());
        }
        customerDAO.update(customer);
        return customer;
    }

    @Override
    public synchronized Account updateAccountStatus(String accountNumber, AccountStatus status) throws AccountNotFoundException {
        Account account = getAccountOrThrow(accountNumber);
        if (status == null) {
            throw new IllegalArgumentException("Account status cannot be null.");
        }
        account.setStatus(status);
        accountDAO.update(account);
        return account;
    }

    @Override
    public List<Customer> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllCustomers();
        }
        String q = query.trim().toLowerCase();
        return customerRegistry.values().stream()
                .filter(c -> c.getCustomerId().toLowerCase().contains(q) ||
                        c.getFullName().toLowerCase().contains(q) ||
                        c.getEmail().toLowerCase().contains(q) ||
                        c.getPhoneNumber().contains(q))
                .collect(Collectors.toList());
    }

    public CustomerDAO getCustomerDAO() {
        return customerDAO;
    }

    public AccountDAO getAccountDAO() {
        return accountDAO;
    }

    public TransactionDAO getTransactionDAO() {
        return transactionDAO;
    }

    // ==================== Helper Lookup Routines ====================

    private Customer getCustomerOrThrow(String customerId) throws CustomerNotFoundException {
        if (customerId == null) {
            throw new CustomerNotFoundException("null");
        }
        Customer customer = customerRegistry.get(customerId.trim());
        if (customer == null) {
            // Fallback query to DAO in case not present in memory
            customer = customerDAO.findById(customerId.trim())
                    .orElseThrow(() -> new CustomerNotFoundException(customerId));
            customerRegistry.put(customerId.trim(), customer);
        }
        return customer;
    }

    private Account getAccountOrThrow(String accountNumber) throws AccountNotFoundException {
        if (accountNumber == null) {
            throw new AccountNotFoundException("null");
        }
        Account account = accountRegistry.get(accountNumber.trim());
        if (account == null) {
            // Fallback query to DAO
            account = accountDAO.findByAccountNumber(accountNumber.trim())
                    .orElseThrow(() -> new AccountNotFoundException(accountNumber));
            accountRegistry.put(accountNumber.trim(), account);
        }
        return account;
    }
}

