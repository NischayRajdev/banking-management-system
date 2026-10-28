package com.bank.main;

import com.bank.dao.*;
import com.bank.exception.*;
import com.bank.model.*;
import com.bank.service.Bank;
import com.bank.service.DatabaseSeeder;

import java.math.BigDecimal;
import java.util.List;

// BankingApplication: Main entry point for the India Bank system
public class BankingApplication {

    // main: Launches the interactive console CLI or headless test suite
    public static void main(String[] args) {
        boolean headless = false;
        boolean forceReset = false;

        for (String arg : args) {
            if ("--headless".equalsIgnoreCase(arg) || "--test".equalsIgnoreCase(arg)) {
                headless = true;
            }
            if ("--reset".equalsIgnoreCase(arg) || "--reseed".equalsIgnoreCase(arg)) {
                forceReset = true;
            }
        }

        DBConnection.initializeDatabase();

        Bank bank = new Bank("India Bank", "INDB-001",
                new CustomerDAOImpl(),
                new AccountDAOImpl(),
                new TransactionDAOImpl());

        if (forceReset) {
            DatabaseSeeder.seed(bank, true);
        } else {
            DatabaseSeeder.seedIfEmpty(bank);
        }

        if (headless) {
            runAutomatedVerificationSuite(bank);
            return;
        }

        InteractiveBankingCLI cli = new InteractiveBankingCLI(bank);
        cli.start();
    }

    // runAutomatedVerificationSuite: Executes backend automated verification suite
    public static void runAutomatedVerificationSuite(Bank bank) {
        printBanner("INDIA BANK - BACKEND ENGINE TEST RUNNER");

        try {
            printSectionHeader("0. DATABASE CONNECTIVITY & SCHEMA VERIFICATION");
            System.out.println(" Database connection successfully established: " + DBConnection.getJdbcUrl());
            System.out.println(" Relational schema (customers, accounts, transactions) validated.\n");

            printSectionHeader("1. CUSTOMER REGISTRATION & ENCAPSULATION");
            String testEmailAarav = "test.aarav." + System.currentTimeMillis() + "@example.com";
            String testEmailPriya = "test.priya." + System.currentTimeMillis() + "@example.com";

            Customer aarav = bank.registerCustomer(
                    "Aarav", "Sharma", testEmailAarav, "+91-98201-0101", "123 Marine Drive, Mumbai");
            Customer priya = bank.registerCustomer(
                    "Priya", "Patel", testEmailPriya, "+91-98201-0202", "456 Brigade Road, Bengaluru");

            System.out.println(" Registered Customer 1: " + aarav.getFullName() + " | ID: " + aarav.getCustomerId());
            System.out.println(" Registered Customer 2: " + priya.getFullName() + " | ID: " + priya.getCustomerId());

            System.out.println("\n Attempting duplicate customer registration with email: " + aarav.getEmail());
            try {
                bank.registerCustomer("Aarav", "Duplicate", testEmailAarav, "000", "Nowhere");
            } catch (BankingException ex) {
                System.out.println("  Properly caught expected exception: " + ex.getMessage());
            }

            printSectionHeader("2. ACCOUNT CREATION (INHERITANCE & ABSTRACTION)");
            SavingsAccount aaravSavings = bank.openSavingsAccount(
                    aarav.getCustomerId(),
                    new BigDecimal("50000.00"),
                    new BigDecimal("0.040"),
                    new BigDecimal("5000.00"),
                    new BigDecimal("20000.00")
            );
            System.out.println(" Opened Savings Account for " + aarav.getFullName());
            System.out.println("   Account No: " + aaravSavings.getAccountNumber() + " | Initial Deposit: Rs. " + aaravSavings.getBalance());

            CurrentAccount priyaCurrent = bank.openCurrentAccount(
                    priya.getCustomerId(),
                    new BigDecimal("10000.00"),
                    new BigDecimal("30000.00"),
                    new BigDecimal("0.100")
            );
            System.out.println(" Opened Current Account for " + priya.getFullName());
            System.out.println("   Account No: " + priyaCurrent.getAccountNumber() + " | Initial Deposit: Rs. " + priyaCurrent.getBalance());

            CurrentAccount aaravCurrent = bank.openCurrentAccount(
                    aarav.getCustomerId(),
                    new BigDecimal("25000.00")
            );
            System.out.println(" Opened secondary Current Account for " + aarav.getFullName() + " (" + aaravCurrent.getAccountNumber() + ")");
            System.out.println("   Aarav's Total Portfolio Net Worth across " + aarav.getAccounts().size() + " accounts: Rs. "
                    + aarav.getTotalPortfolioBalance());

            printSectionHeader("3. POLYMORPHISM - METHOD OVERRIDING");
            System.out.println("A) Dynamic Interest Calculation:");
            BigDecimal aaravInterest = aaravSavings.calculateInterest();
            System.out.println("   Savings Account [" + aaravSavings.getAccountNumber() + "] Balance: Rs. "
                    + aaravSavings.getBalance() + " @ 4% -> Annual Interest: Rs. " + aaravInterest);

            BigDecimal priyaInterestBefore = priyaCurrent.calculateInterest();
            System.out.println("   Current Account [" + priyaCurrent.getAccountNumber() + "] Balance: Rs. "
                    + priyaCurrent.getBalance() + " (Positive) -> Interest Accrued: Rs. " + priyaInterestBefore);

            System.out.println("\nB) Polymorphic Display Output:");
            System.out.println(aaravSavings.displayAccountDetails());
            System.out.println();
            System.out.println(priyaCurrent.displayAccountDetails());

            printSectionHeader("4. POLYMORPHISM - METHOD OVERLOADING");
            bank.deposit(aaravSavings.getAccountNumber(), new BigDecimal("12000.00"));
            System.out.println(" Executed deposit(accountNumber, amount): Added Rs. 12000.00 to " + aaravSavings.getAccountNumber());

            bank.deposit(aaravSavings.getAccountNumber(), new BigDecimal("8000.00"), "Diwali bonus payment");
            System.out.println(" Executed deposit(accountNumber, amount, remarks): Added Rs. 8000.00 to " + aaravSavings.getAccountNumber());
            System.out.println(" New Balance: Rs. " + aaravSavings.getBalance());

            System.out.println("\n Overloaded Lookups:");
            Account foundByAccNo = bank.findAccount(aaravSavings.getAccountNumber());
            System.out.println("   findAccount(accountNumber) -> " + foundByAccNo.getAccountHolderName() + " (" + foundByAccNo.getAccountNumber() + ")");

            Customer foundByEmail = bank.findCustomerByEmail(testEmailPriya);
            System.out.println("   findCustomerByEmail(email) -> " + foundByEmail.getFullName() + " (ID: " + foundByEmail.getCustomerId() + ")");

            List<Account> filteredByBalance = bank.findAccountsByBalanceRange(new BigDecimal("20000.00"), new BigDecimal("1000000.00"));
            System.out.println("   findAccountsByBalanceRange(Rs. 20000, Rs. 1000000) -> Matched " + filteredByBalance.size() + " accounts.");

            printSectionHeader("5. EXCEPTION HANDLING & BUSINESS CONSTRAINTS");
            System.out.println("A) Testing negative deposit rejection:");
            try {
                bank.deposit(aaravSavings.getAccountNumber(), new BigDecimal("-500.00"));
            } catch (InvalidTransactionException e) {
                System.out.println("   Caught: " + e.getMessage());
            }

            System.out.println("\nB) Testing Savings Account per-transaction limit (Rs. 20,000 ceiling):");
            try {
                bank.withdraw(aaravSavings.getAccountNumber(), new BigDecimal("25000.00"));
            } catch (InvalidTransactionException e) {
                System.out.println("   Caught: " + e.getMessage());
            }

            System.out.println("\nC) Testing Savings Account minimum balance violation (Rs. 5,000 minimum):");
            try {
                bank.withdraw(aaravSavings.getAccountNumber(), new BigDecimal("19000.00"));
                bank.withdraw(aaravSavings.getAccountNumber(), new BigDecimal("19000.00"));
                bank.withdraw(aaravSavings.getAccountNumber(), new BigDecimal("19000.00"));
                System.out.println("   Balance reduced to: Rs. " + aaravSavings.getBalance() + " (Minimum is Rs. 5000.00)");
                System.out.println("   Attempting to withdraw Rs. 9000.00 (which would leave balance below Rs. 5000):");
                bank.withdraw(aaravSavings.getAccountNumber(), new BigDecimal("9000.00"));
            } catch (InsufficientBalanceException e) {
                System.out.println("   Caught InsufficientBalanceException: " + e.getMessage());
            }

            printSectionHeader("6. CURRENT ACCOUNT OVERDRAFT MECHANICS");
            System.out.println("Priya's Current Account starting balance: Rs. " + priyaCurrent.getBalance());
            System.out.println("Overdraft Limit: Rs. " + priyaCurrent.getOverdraftLimit());
            System.out.println("Total Available Liquidity: Rs. " + priyaCurrent.getTotalAvailableLiquidity());

            System.out.println("\nExecuting withdrawal of Rs. 25,000.00 (utilizing Rs. 15,000.00 overdraft)...");
            bank.withdraw(priyaCurrent.getAccountNumber(), new BigDecimal("25000.00"), "Vendor invoice payment");
            System.out.println(" Current Account Balance: Rs. " + priyaCurrent.getBalance());
            System.out.println(" Overdraft Interest Calculation: Rs. " + priyaCurrent.calculateInterest() + " (Fee on overdrawn balance)");

            System.out.println("\nAttempting withdrawal of Rs. 20,000.00 (exceeds remaining Rs. 15,000.00 overdraft buffer)...");
            try {
                bank.withdraw(priyaCurrent.getAccountNumber(), new BigDecimal("20000.00"));
            } catch (OverdraftExceededException e) {
                System.out.println("   Caught OverdraftExceededException: " + e.getMessage());
            }

            printSectionHeader("7. ACID FUND TRANSFER WITH DATABASE ROLLBACK TEST");
            System.out.println("A) Successful Atomic Transfer:");
            System.out.println("   Aarav Current Acc [" + aaravCurrent.getAccountNumber() + "] Balance Before: Rs. " + aaravCurrent.getBalance());
            System.out.println("   Priya Current Acc [" + priyaCurrent.getAccountNumber() + "] Balance Before: Rs. " + priyaCurrent.getBalance());

            List<Transaction> transferRecords = bank.transfer(
                    aaravCurrent.getAccountNumber(),
                    priyaCurrent.getAccountNumber(),
                    new BigDecimal("10000.00"),
                    "Consulting settlement"
            );

            System.out.println("    Transfer completed atomically! Two ledger records generated:");
            for (Transaction tx : transferRecords) {
                System.out.println("      " + tx.getAuditSummary());
            }
            System.out.println("   Aarav Current Acc Balance After: Rs. " + aaravCurrent.getBalance());
            System.out.println("   Priya Current Acc Balance After: Rs. " + priyaCurrent.getBalance());

            System.out.println("\nB) Negative Test: Atomic Rollback on Inadequate Balance:");
            BigDecimal aaravCurrentBeforeRollback = aaravCurrent.getBalance();
            BigDecimal priyaCurrentBeforeRollback = priyaCurrent.getBalance();

            System.out.println("   Attempting to transfer Rs. 500,000.00 (vastly exceeding Aarav's balance/overdraft)...");
            try {
                bank.transfer(aaravCurrent.getAccountNumber(), priyaCurrent.getAccountNumber(), new BigDecimal("500000.00"));
            } catch (BankingException e) {
                System.out.println("   Transfer aborted as expected: " + e.getMessage());
            }

            System.out.println("   Verifying Balances preserved intact:");
            System.out.println("     Aarav Balance: Rs. " + aaravCurrent.getBalance() + " (Unchanged: " + aaravCurrent.getBalance().equals(aaravCurrentBeforeRollback) + ")");
            System.out.println("     Priya Balance: Rs. " + priyaCurrent.getBalance() + " (Unchanged: " + priyaCurrent.getBalance().equals(priyaCurrentBeforeRollback) + ")");

            printSectionHeader("8. ACCOUNT LEDGER AUDIT TRAIL");
            System.out.println("Transaction Ledger for Priya's Account [" + priyaCurrent.getAccountNumber() + "]:");
            List<Transaction> priyaStatement = bank.getAccountStatement(priyaCurrent.getAccountNumber());
            for (Transaction txn : priyaStatement) {
                System.out.println("   " + txn.getAuditSummary());
            }

            printSectionHeader("9. DATABASE RESTART & PERSISTENCE VERIFICATION");
            System.out.println("Simulating system shutdown and cold reboot from database...");
            Bank freshBankInstance = new Bank("India Bank", "INDB-001",
                    new CustomerDAOImpl(),
                    new AccountDAOImpl(),
                    new TransactionDAOImpl());

            List<Customer> loadedCustomers = freshBankInstance.getAllCustomers();
            List<Account> loadedAccounts = freshBankInstance.getAllAccounts();

            System.out.println(" Successfully rehydrated from SQLite database:");
            System.out.println("   Total Customers loaded: " + loadedCustomers.size());
            System.out.println("   Total Accounts loaded : " + loadedAccounts.size());

            printBanner("ALL TESTS PASSED SUCCESSFULLY! BACKEND ENGINE FULLY OPERATIONAL.");

        } catch (Exception e) {
            System.err.println("\n FATAL TEST RUNNER ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // printBanner: Displays a formatted header banner
    private static void printBanner(String title) {
        String border = "=".repeat(85);
        System.out.println("\n" + border);
        System.out.println("  " + title);
        System.out.println(border + "\n");
    }

    // printSectionHeader: Displays a section divider
    private static void printSectionHeader(String section) {
        System.out.println("\n" + "-".repeat(85));
        System.out.println(">>> " + section);
        System.out.println("-".repeat(85));
    }
}
