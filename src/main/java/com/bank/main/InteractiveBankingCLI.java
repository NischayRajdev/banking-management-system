package com.bank.main;

import com.bank.dao.DBConnection;
import com.bank.exception.*;
import com.bank.model.*;
import com.bank.service.Bank;
import com.bank.service.DatabaseSeeder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.*;

// InteractiveBankingCLI: Interactive Console Terminal UI for India Bank
public class InteractiveBankingCLI {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Bank bank;
    private final Scanner scanner;

    public InteractiveBankingCLI(Bank bank) {
        this.bank = bank;
        this.scanner = new Scanner(System.in);
    }

    // main: Direct entry point delegating to BankingApplication
    public static void main(String[] args) {
        BankingApplication.main(args);
    }

    // start: Main interactive application loop
    public void start() {
        printBanner("INDIA BANKING SYSTEM - INTERACTIVE CONSOLE");

        boolean running = true;
        while (running) {
            printDashboardHeader();
            System.out.println(">>> MAIN NAVIGATION MENU <<<");
            System.out.println("  [1] Customer Management       (Register, View All, Search, Portfolio, Update)");
            System.out.println("  [2] Account Management        (Open Savings/Current, View All, Filter, Status)");
            System.out.println("  [3] Financial Transactions    (Deposit, Withdraw, Transfer, Interest, Statement)");
            System.out.println("  [4] Analytics & Bank Reports  (Vault Summary, Wealth Ranking, Overdrawn List)");
            System.out.println("  [5] Database & Seeder Tools   (Seed Demo Data, Add More Users, Reset DB)");
            System.out.println("  [6] Run Automated Test Suite  (Execute OOP & ACID Verification Tests)");
            System.out.println("  [0] Exit Application");
            System.out.println("-".repeat(80));

            int choice = promptInt("Select an option [0-6]", 0, 6);
            System.out.println();

            switch (choice) {
                case 1 -> customerManagementMenu();
                case 2 -> accountManagementMenu();
                case 3 -> transactionManagementMenu();
                case 4 -> analyticsMenu();
                case 5 -> databaseToolsMenu();
                case 6 -> runAutomatedTests();
                case 0 -> {
                    printSuccess("Thank you for using India Bank. Goodbye!");
                    running = false;
                }
            }
        }
    }

    // =========================================================================
    // 1. CUSTOMER MANAGEMENT SUB-MENU
    // =========================================================================

    private void customerManagementMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n" + "=".repeat(75));
            System.out.println("                    CUSTOMER MANAGEMENT");
            System.out.println("=".repeat(75));
            System.out.println("  [1] Register New Customer");
            System.out.println("  [2] View All Customers (Summary Table)");
            System.out.println("  [3] Search Customer by ID / Email / Name");
            System.out.println("  [4] View Customer Portfolio & Associated Accounts");
            System.out.println("  [5] Update Customer Contact Information");
            System.out.println("  [0] << Return to Main Menu");
            System.out.println("-".repeat(75));

            int choice = promptInt("Enter choice [0-5]", 0, 5);
            System.out.println();

            switch (choice) {
                case 1 -> registerNewCustomerFlow();
                case 2 -> displayAllCustomersTable();
                case 3 -> searchCustomerFlow();
                case 4 -> viewCustomerPortfolioFlow();
                case 5 -> updateCustomerContactFlow();
                case 0 -> inMenu = false;
            }
        }
    }

    private void registerNewCustomerFlow() {
        printSubHeader("REGISTER NEW CUSTOMER");
        System.out.println("(Type 'cancel' at any prompt to return)");

        String firstName = promptNonBlank("First Name");
        if (isCancel(firstName)) return;

        String lastName = promptNonBlank("Last Name");
        if (isCancel(lastName)) return;

        String email = promptEmail("Email Address");
        if (isCancel(email)) return;

        String phone = promptString("Phone Number (e.g. +1-555-0100)", false);
        if (isCancel(phone)) return;

        String address = promptString("Residential / Business Address", false);
        if (isCancel(address)) return;

        try {
            Customer customer = bank.registerCustomer(firstName, lastName, email, phone, address);
            printSuccess("Customer Registered Successfully!");
            System.out.println("  Customer ID : " + customer.getCustomerId());
            System.out.println("  Full Name   : " + customer.getFullName());
            System.out.println("  Email       : " + customer.getEmail());
            System.out.println("  Phone       : " + customer.getPhoneNumber());
            System.out.println("  Address     : " + customer.getAddress());

            // Offer immediate account opening
            System.out.println("\nWould you like to open an account for this customer now?");
            System.out.println("  [1] Open Savings Account");
            System.out.println("  [2] Open Current (Checking) Account");
            System.out.println("  [0] Skip / Do it later");
            int actChoice = promptInt("Select option [0-2]", 0, 2);
            if (actChoice == 1) {
                openSavingsAccountForCustomer(customer.getCustomerId());
            } else if (actChoice == 2) {
                openCurrentAccountForCustomer(customer.getCustomerId());
            }
        } catch (BankingException e) {
            printError("Registration Failed: " + e.getMessage());
        }
    }

    private void displayAllCustomersTable() {
        printSubHeader("REGISTERED CUSTOMERS DIRECTORY");
        List<Customer> customers = new ArrayList<>(bank.getAllCustomers());
        if (customers.isEmpty()) {
            printInfo("No customers registered in database yet. Try seeding sample data via menu [5].");
            return;
        }

        customers.sort(Comparator.comparing(Customer::getCustomerId));
        String border = "+-----------+----------------------+-----------------------------+-----------------+----------+--------------------+";
        System.out.println(border);
        System.out.printf("| %-9s | %-20s | %-27s | %-15s | %-8s | %-18s |\n",
                "Cust ID", "Full Name", "Email", "Phone", "Accounts", "Net Worth");
        System.out.println(border);

        for (Customer c : customers) {
            System.out.printf("| %-9s | %-20s | %-27s | %-15s | %-8d | Rs. %14s |\n",
                    c.getCustomerId(),
                    truncate(c.getFullName(), 20),
                    truncate(c.getEmail(), 27),
                    truncate(c.getPhoneNumber(), 15),
                    c.getAccounts().size(),
                    formatMoney(c.getTotalPortfolioBalance()));
        }
        System.out.println(border);
        System.out.printf(" Total Customers: %d\n", customers.size());
    }

    private void searchCustomerFlow() {
        printSubHeader("SEARCH CUSTOMER");
        String query = promptNonBlank("Enter Customer ID, Email, or Full Name to search");
        if (isCancel(query)) return;

        List<Customer> matches = bank.searchCustomers(query);
        if (matches.isEmpty()) {
            printWarning("No customers found matching: '" + query + "'");
            return;
        }

        printSuccess("Found " + matches.size() + " matching customer(s):");
        for (Customer c : matches) {
            printCustomerCard(c);
        }
    }

    private void viewCustomerPortfolioFlow() {
        printSubHeader("VIEW CUSTOMER PORTFOLIO & ACCOUNTS");
        String custId = promptNonBlank("Enter Customer ID (e.g. CUST-1001)");
        if (isCancel(custId)) return;

        try {
            Customer c = bank.findCustomer(custId);
            printCustomerCard(c);

            List<Account> accounts = c.getAccounts();
            if (accounts.isEmpty()) {
                printInfo("Customer does not currently have any associated accounts.");
            } else {
                System.out.println("\nAssociated Accounts Portfolio (" + accounts.size() + " accounts):");
                String border = "+-------------------+-----------+---------------------+-------------------------+----------+";
                System.out.println(border);
                System.out.printf("| %-17s | %-9s | %-19s | %-23s | %-8s |\n",
                        "Account Number", "Type", "Balance", "Available Liquidity", "Status");
                System.out.println(border);

                for (Account acc : accounts) {
                    BigDecimal avail = (acc instanceof CurrentAccount ca)
                            ? ca.getTotalAvailableLiquidity()
                            : acc.getBalance();
                    System.out.printf("| %-17s | %-9s | Rs. %15s | Rs. %19s | %-8s |\n",
                            acc.getAccountNumber(),
                            acc.getAccountType(),
                            formatMoney(acc.getBalance()),
                            formatMoney(avail),
                            acc.getStatus());
                }
                System.out.println(border);
                System.out.printf("Total Portfolio Net Worth across all accounts: Rs. %s\n",
                        formatMoney(c.getTotalPortfolioBalance()));
            }
        } catch (CustomerNotFoundException e) {
            printError("Customer not found: " + e.getMessage());
        }
    }

    private void updateCustomerContactFlow() {
        printSubHeader("UPDATE CUSTOMER CONTACT INFORMATION");
        String custId = promptNonBlank("Enter Customer ID to update");
        if (isCancel(custId)) return;

        try {
            Customer c = bank.findCustomer(custId);
            System.out.println("Updating details for: " + c.getFullName() + " (" + c.getCustomerId() + ")");
            System.out.println("Current Phone  : " + c.getPhoneNumber());
            System.out.println("Current Address: " + c.getAddress());
            System.out.println("(Press Enter without typing to keep existing value)");

            String newPhone = promptString("New Phone Number", true);
            if (newPhone.trim().isEmpty()) newPhone = c.getPhoneNumber();

            String newAddress = promptString("New Address", true);
            if (newAddress.trim().isEmpty()) newAddress = c.getAddress();

            bank.updateCustomer(custId, c.getFirstName(), c.getLastName(), newPhone, newAddress);
            printSuccess("Customer contact details updated successfully!");
        } catch (CustomerNotFoundException e) {
            printError("Customer not found: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. ACCOUNT MANAGEMENT SUB-MENU
    // =========================================================================

    private void accountManagementMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n" + "=".repeat(75));
            System.out.println("                     ACCOUNT MANAGEMENT");
            System.out.println("=".repeat(75));
            System.out.println("  [1] Open New Savings Account");
            System.out.println("  [2] Open New Current Account");
            System.out.println("  [3] View All Accounts Directory");
            System.out.println("  [4] View Account Details & Specifications");
            System.out.println("  [5] Filter Accounts by Balance Range");
            System.out.println("  [6] Modify Account Status (ACTIVE, FROZEN, CLOSED)");
            System.out.println("  [0] << Return to Main Menu");
            System.out.println("-".repeat(75));

            int choice = promptInt("Enter choice [0-6]", 0, 6);
            System.out.println();

            switch (choice) {
                case 1 -> openSavingsAccountFlow();
                case 2 -> openCurrentAccountFlow();
                case 3 -> displayAllAccountsTable();
                case 4 -> viewAccountDetailsFlow();
                case 5 -> filterAccountsByBalanceFlow();
                case 6 -> modifyAccountStatusFlow();
                case 0 -> inMenu = false;
            }
        }
    }

    private void openSavingsAccountFlow() {
        printSubHeader("OPEN NEW SAVINGS ACCOUNT");
        String custId = promptNonBlank("Enter Customer ID (or 'list' to view customers)");
        if (isCancel(custId)) return;

        if (custId.equalsIgnoreCase("list")) {
            displayAllCustomersTable();
            custId = promptNonBlank("Enter Customer ID");
            if (isCancel(custId)) return;
        }

        openSavingsAccountForCustomer(custId);
    }

    private void openSavingsAccountForCustomer(String custId) {
        try {
            Customer cust = bank.findCustomer(custId);
            System.out.println("Opening Savings Account for: " + cust.getFullName());

            BigDecimal minBal = SavingsAccount.DEFAULT_MINIMUM_BALANCE;
            System.out.println("Minimum required opening balance: Rs. " + formatMoney(minBal));

            BigDecimal initialDeposit = promptBigDecimal("Initial Deposit Amount (Rs.)", minBal, new BigDecimal("100000000.00"));
            if (initialDeposit == null) return;

            System.out.println("Standard Interest Rate: 4.5% annual (0.045)");
            String rateInput = promptString("Custom Interest Rate decimal [Enter for default 0.045]", true);
            BigDecimal interestRate = rateInput.trim().isEmpty() ? SavingsAccount.DEFAULT_INTEREST_RATE : new BigDecimal(rateInput.trim());

            SavingsAccount acc = bank.openSavingsAccount(
                    custId, initialDeposit, interestRate, minBal, SavingsAccount.DEFAULT_MAX_WITHDRAWAL_LIMIT
            );

            printSuccess("Savings Account Created Successfully!");
            System.out.println("  Account Number    : " + acc.getAccountNumber());
            System.out.println("  Account Holder    : " + acc.getAccountHolderName());
            System.out.println("  Initial Balance   : Rs. " + formatMoney(acc.getBalance()));
            System.out.println("  Interest Rate     : " + acc.getInterestRate().multiply(new BigDecimal("100")) + "%");
            System.out.println("  Minimum Balance   : Rs. " + formatMoney(acc.getMinimumBalance()));
            System.out.println("  Max Withdrawal/Txn: Rs. " + formatMoney(acc.getMaxWithdrawalPerTransaction()));
            System.out.println("  Account Status    : " + acc.getStatus());

        } catch (BankingException e) {
            printError("Failed to open savings account: " + e.getMessage());
        }
    }

    private void openCurrentAccountFlow() {
        printSubHeader("OPEN NEW CURRENT (CHECKING) ACCOUNT");
        String custId = promptNonBlank("Enter Customer ID (or 'list' to view customers)");
        if (isCancel(custId)) return;

        if (custId.equalsIgnoreCase("list")) {
            displayAllCustomersTable();
            custId = promptNonBlank("Enter Customer ID");
            if (isCancel(custId)) return;
        }

        openCurrentAccountForCustomer(custId);
    }

    private void openCurrentAccountForCustomer(String custId) {
        try {
            Customer cust = bank.findCustomer(custId);
            System.out.println("Opening Current Account for: " + cust.getFullName());

            BigDecimal initialDeposit = promptBigDecimal("Initial Deposit Amount (Rs.) [Can be 0.00]", BigDecimal.ZERO, new BigDecimal("100000000.00"));
            if (initialDeposit == null) return;

            System.out.println("Default Overdraft Credit Facility: Rs. 50,000.00 @ 12.0% APR");
            String overdraftStr = promptString("Custom Overdraft Limit (Rs.) [Enter for default 50000.00]", true);
            BigDecimal overdraftLimit = overdraftStr.trim().isEmpty() ? CurrentAccount.DEFAULT_OVERDRAFT_LIMIT : new BigDecimal(overdraftStr.trim());

            CurrentAccount acc = bank.openCurrentAccount(
                    custId, initialDeposit, overdraftLimit, CurrentAccount.DEFAULT_OVERDRAFT_INTEREST_RATE
            );

            printSuccess("Current Account Created Successfully!");
            System.out.println("  Account Number     : " + acc.getAccountNumber());
            System.out.println("  Account Holder     : " + acc.getAccountHolderName());
            System.out.println("  Initial Balance    : Rs. " + formatMoney(acc.getBalance()));
            System.out.println("  Overdraft Buffer   : Rs. " + formatMoney(acc.getOverdraftLimit()));
            System.out.println("  Total Liquidity    : Rs. " + formatMoney(acc.getTotalAvailableLiquidity()));
            System.out.println("  Overdraft Fee Rate : " + acc.getOverdraftInterestRate().multiply(new BigDecimal("100")) + "% APR");
            System.out.println("  Account Status     : " + acc.getStatus());

        } catch (BankingException e) {
            printError("Failed to open current account: " + e.getMessage());
        }
    }

    private void displayAllAccountsTable() {
        printSubHeader("INSTITUTION ACCOUNTS DIRECTORY");
        List<Account> accounts = new ArrayList<>(bank.getAllAccounts());
        if (accounts.isEmpty()) {
            printInfo("No accounts currently registered in database.");
            return;
        }

        accounts.sort(Comparator.comparing(Account::getAccountNumber));
        String border = "+-------------------+----------------------+-----------+---------+--------------------+--------------------+----------+";
        System.out.println(border);
        System.out.printf("| %-17s | %-20s | %-9s | %-7s | %-18s | %-18s | %-8s |\n",
                "Account Number", "Account Holder", "Cust ID", "Type", "Balance", "Available Funds", "Status");
        System.out.println(border);

        BigDecimal totalVaultAssets = BigDecimal.ZERO;
        for (Account a : accounts) {
            BigDecimal avail = (a instanceof CurrentAccount ca) ? ca.getTotalAvailableLiquidity() : a.getBalance();
            totalVaultAssets = totalVaultAssets.add(a.getBalance());
            System.out.printf("| %-17s | %-20s | %-9s | %-7s | Rs. %14s | Rs. %14s | %-8s |\n",
                    a.getAccountNumber(),
                    truncate(a.getAccountHolderName(), 20),
                    a.getCustomerId(),
                    a.getAccountType(),
                    formatMoney(a.getBalance()),
                    formatMoney(avail),
                    a.getStatus());
        }
        System.out.println(border);
        System.out.printf(" Total Accounts: %d | Total Vault Net Balance: Rs. %s\n",
                accounts.size(), formatMoney(totalVaultAssets));
    }

    private void viewAccountDetailsFlow() {
        printSubHeader("VIEW DETAILED ACCOUNT SPECIFICATIONS");
        String accNum = promptNonBlank("Enter Account Number (e.g. SAV-INDB-001-5001 or CUR-INDB-001-8001)");
        if (isCancel(accNum)) return;

        try {
            Account acc = bank.findAccount(accNum);
            System.out.println("\n" + acc.displayAccountDetails());
            System.out.println("Dynamic Annual Interest Accrual / Charge Calculation: Rs. " + formatMoney(acc.calculateInterest()));
        } catch (AccountNotFoundException e) {
            printError("Account not found: " + e.getMessage());
        }
    }

    private void filterAccountsByBalanceFlow() {
        printSubHeader("FILTER ACCOUNTS BY BALANCE RANGE");
        BigDecimal min = promptBigDecimal("Minimum Balance Bound (Rs.)", new BigDecimal("-10000000.00"), new BigDecimal("100000000.00"));
        if (min == null) return;

        BigDecimal max = promptBigDecimal("Maximum Balance Bound (Rs.)", min, new BigDecimal("100000000.00"));
        if (max == null) return;

        List<Account> matches = bank.findAccountsByBalanceRange(min, max);
        if (matches.isEmpty()) {
            printWarning("No accounts found with balance between Rs. " + formatMoney(min) + " and Rs. " + formatMoney(max));
            return;
        }

        printSuccess("Found " + matches.size() + " accounts matching range [Rs. " + formatMoney(min) + " - Rs. " + formatMoney(max) + "]:");
        String border = "+-------------------+----------------------+---------+--------------------+----------+";
        System.out.println(border);
        System.out.printf("| %-17s | %-20s | %-7s | %-18s | %-8s |\n",
                "Account Number", "Account Holder", "Type", "Balance", "Status");
        System.out.println(border);
        for (Account a : matches) {
            System.out.printf("| %-17s | %-20s | %-7s | Rs. %14s | %-8s |\n",
                    a.getAccountNumber(), truncate(a.getAccountHolderName(), 20),
                    a.getAccountType(), formatMoney(a.getBalance()), a.getStatus());
        }
        System.out.println(border);
    }

    private void modifyAccountStatusFlow() {
        printSubHeader("MODIFY ACCOUNT STATUS");
        String accNum = promptNonBlank("Enter Account Number");
        if (isCancel(accNum)) return;

        try {
            Account acc = bank.findAccount(accNum);
            System.out.println("Account: " + acc.getAccountNumber() + " | Holder: " + acc.getAccountHolderName());
            System.out.println("Current Status: " + acc.getStatus());

            System.out.println("\nSelect New Status:");
            System.out.println("  [1] ACTIVE");
            System.out.println("  [2] FROZEN");
            System.out.println("  [3] CLOSED");
            System.out.println("  [0] Cancel");
            int stChoice = promptInt("Select status [0-3]", 0, 3);
            AccountStatus newStatus = switch (stChoice) {
                case 1 -> AccountStatus.ACTIVE;
                case 2 -> AccountStatus.FROZEN;
                case 3 -> AccountStatus.CLOSED;
                default -> null;
            };

            if (newStatus != null) {
                bank.updateAccountStatus(accNum, newStatus);
                printSuccess("Account status updated to: " + newStatus);
            }
        } catch (AccountNotFoundException e) {
            printError("Account not found: " + e.getMessage());
        }
    }

    // =========================================================================
    // 3. FINANCIAL TRANSACTIONS SUB-MENU
    // =========================================================================

    private void transactionManagementMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n" + "=".repeat(75));
            System.out.println("                FINANCIAL TRANSACTIONS & OPERATIONS");
            System.out.println("=".repeat(75));
            System.out.println("  [1] Deposit Funds (Credit)");
            System.out.println("  [2] Withdraw Funds (Debit)");
            System.out.println("  [3] Fund Transfer (Inter-Account ACID Transaction)");
            System.out.println("  [4] Calculate & Post Interest / Overdraft Charge");
            System.out.println("  [5] View Account Ledger Statement");
            System.out.println("  [0] << Return to Main Menu");
            System.out.println("-".repeat(75));

            int choice = promptInt("Enter choice [0-5]", 0, 5);
            System.out.println();

            switch (choice) {
                case 1 -> depositFlow();
                case 2 -> withdrawFlow();
                case 3 -> transferFlow();
                case 4 -> interestPostingFlow();
                case 5 -> accountStatementFlow();
                case 0 -> inMenu = false;
            }
        }
    }

    private void depositFlow() {
        printSubHeader("DEPOSIT FUNDS (CREDIT)");
        String accNum = promptNonBlank("Enter Account Number");
        if (isCancel(accNum)) return;

        try {
            Account acc = bank.findAccount(accNum);
            System.out.println("Account Holder  : " + acc.getAccountHolderName());
            System.out.println("Account Type    : " + acc.getAccountType());
            System.out.println("Current Balance : Rs. " + formatMoney(acc.getBalance()));

            BigDecimal amount = promptBigDecimal("Enter Deposit Amount (Rs.)", new BigDecimal("0.01"), new BigDecimal("100000000.00"));
            if (amount == null) return;

            String remarks = promptString("Enter Remarks / Memo [Optional]", true);
            if (remarks.trim().isEmpty()) remarks = "Standard Cash/Online Deposit";

            BigDecimal prevBalance = acc.getBalance();
            Transaction txn = bank.deposit(accNum, amount, remarks);

            printTransactionReceipt(
                    "DEPOSIT CONFIRMATION RECEIPT",
                    txn.getTransactionId(),
                    txn.getTimestamp(),
                    "CREDIT / DEPOSIT",
                    acc.getAccountNumber(),
                    acc.getAccountHolderName(),
                    null,
                    amount,
                    prevBalance,
                    txn.getBalanceAfter(),
                    txn.getStatus().name(),
                    remarks
            );

        } catch (BankingException e) {
            printError("Deposit Rejected: " + e.getMessage());
        }
    }

    private void withdrawFlow() {
        printSubHeader("WITHDRAW FUNDS (DEBIT)");
        String accNum = promptNonBlank("Enter Account Number");
        if (isCancel(accNum)) return;

        try {
            Account acc = bank.findAccount(accNum);
            System.out.println("Account Holder      : " + acc.getAccountHolderName());
            System.out.println("Account Type        : " + acc.getAccountType());
            System.out.println("Current Balance     : Rs. " + formatMoney(acc.getBalance()));

            if (acc instanceof CurrentAccount ca) {
                System.out.println("Overdraft Limit     : Rs. " + formatMoney(ca.getOverdraftLimit()));
                System.out.println("Total Available Liquidity: Rs. " + formatMoney(ca.getTotalAvailableLiquidity()));
            } else if (acc instanceof SavingsAccount sa) {
                System.out.println("Required Min Balance: Rs. " + formatMoney(sa.getMinimumBalance()));
                System.out.println("Max Per Transaction : Rs. " + formatMoney(sa.getMaxWithdrawalPerTransaction()));
            }

            BigDecimal amount = promptBigDecimal("Enter Withdrawal Amount (Rs.)", new BigDecimal("0.01"), new BigDecimal("100000000.00"));
            if (amount == null) return;

            String remarks = promptString("Enter Remarks / Memo [Optional]", true);
            if (remarks.trim().isEmpty()) remarks = "ATM / Teller Withdrawal";

            BigDecimal prevBalance = acc.getBalance();
            Transaction txn = bank.withdraw(accNum, amount, remarks);

            printTransactionReceipt(
                    "WITHDRAWAL DISBURSEMENT RECEIPT",
                    txn.getTransactionId(),
                    txn.getTimestamp(),
                    "DEBIT / WITHDRAWAL",
                    acc.getAccountNumber(),
                    acc.getAccountHolderName(),
                    null,
                    amount,
                    prevBalance,
                    txn.getBalanceAfter(),
                    txn.getStatus().name(),
                    remarks
            );

            if (txn.getBalanceAfter().compareTo(BigDecimal.ZERO) < 0 && acc instanceof CurrentAccount ca) {
                printWarning("NOTICE: Account is now OVERDRAWN by Rs. " + formatMoney(txn.getBalanceAfter().abs())
                        + ". Remaining Overdraft Buffer: Rs. " + formatMoney(ca.getTotalAvailableLiquidity()));
            }

        } catch (BankingException e) {
            printError("Withdrawal Denied: " + e.getMessage());
        }
    }

    private void transferFlow() {
        printSubHeader("ACID FUND TRANSFER (INTER-ACCOUNT)");
        String srcAccNum = promptNonBlank("Enter Sender / Source Account Number");
        if (isCancel(srcAccNum)) return;

        try {
            Account source = bank.findAccount(srcAccNum);
            System.out.println("Source Holder  : " + source.getAccountHolderName());
            System.out.println("Source Balance : Rs. " + formatMoney(source.getBalance()));

            String dstAccNum = promptNonBlank("Enter Destination / Recipient Account Number");
            if (isCancel(dstAccNum)) return;

            Account dest = bank.findAccount(dstAccNum);
            System.out.println("Destination Holder: " + dest.getAccountHolderName());

            BigDecimal amount = promptBigDecimal("Transfer Amount (Rs.)", new BigDecimal("0.01"), new BigDecimal("100000000.00"));
            if (amount == null) return;

            String remarks = promptString("Transfer Memo / Remarks", true);
            if (remarks.trim().isEmpty()) remarks = "Wire Fund Transfer";

            System.out.printf("\nCONFIRM TRANSFER:\n  Send   : Rs. %s\n  From   : %s (%s)\n  To     : %s (%s)\n  Memo   : %s\n",
                    formatMoney(amount), source.getAccountNumber(), source.getAccountHolderName(),
                    dest.getAccountNumber(), dest.getAccountHolderName(), remarks);

            if (!promptYesNo("Confirm and execute atomic transfer?")) {
                printInfo("Transfer aborted by user.");
                return;
            }

            List<Transaction> txns = bank.transfer(srcAccNum, dstAccNum, amount, remarks);
            printSuccess("Fund Transfer Completed Successfully with ACID Commit!");

            System.out.println("\n" + "=".repeat(75));
            System.out.println("                     DUAL-LEG AUDIT RECEIPT");
            System.out.println("=".repeat(75));
            for (Transaction t : txns) {
                System.out.println("  " + t.getAuditSummary());
            }
            System.out.println("-".repeat(75));
            System.out.println("  Sender New Balance    [" + source.getAccountNumber() + "]: Rs. " + formatMoney(source.getBalance()));
            System.out.println("  Recipient New Balance [" + dest.getAccountNumber() + "]: Rs. " + formatMoney(dest.getBalance()));
            System.out.println("=".repeat(75));

        } catch (BankingException e) {
            printError("Fund Transfer Failed & Rolled Back: " + e.getMessage());
        }
    }

    private void interestPostingFlow() {
        printSubHeader("CALCULATE & POST INTEREST / CHARGES");
        String accNum = promptNonBlank("Enter Account Number");
        if (isCancel(accNum)) return;

        try {
            Account acc = bank.findAccount(accNum);
            System.out.println("Account: " + acc.getAccountNumber() + " (" + acc.getAccountType() + ")");
            System.out.println("Holder : " + acc.getAccountHolderName());
            System.out.println("Balance: Rs. " + formatMoney(acc.getBalance()));

            BigDecimal interest = acc.calculateInterest();
            if (acc instanceof SavingsAccount sa) {
                System.out.println("Savings Interest Rate: " + sa.getInterestRate().multiply(new BigDecimal("100")) + "%");
                System.out.println("Accrued Interest to Credit: Rs. " + formatMoney(interest));
            } else if (acc instanceof CurrentAccount ca) {
                if (ca.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("Overdrawn Balance: Rs. " + formatMoney(ca.getBalance().abs()));
                    System.out.println("Overdraft APR    : " + ca.getOverdraftInterestRate().multiply(new BigDecimal("100")) + "%");
                    System.out.println("Computed Facility Fee to Debit: Rs. " + formatMoney(interest));
                } else {
                    printInfo("Current Account has positive balance. Standard checking accounts do not accrue interest charges.");
                    return;
                }
            }

            if (!promptYesNo("Proceed with posting transaction to database?")) {
                printInfo("Interest posting cancelled.");
                return;
            }

            Transaction txn = bank.applyInterest(accNum);
            printSuccess("Transaction Posted Successfully!");
            System.out.println("  Ledger Record : " + txn.getAuditSummary());
            System.out.println("  New Balance   : Rs. " + formatMoney(acc.getBalance()));

        } catch (BankingException e) {
            printError("Operation Failed: " + e.getMessage());
        }
    }

    private void accountStatementFlow() {
        printSubHeader("ACCOUNT LEDGER AUDIT STATEMENT");
        String accNum = promptNonBlank("Enter Account Number");
        if (isCancel(accNum)) return;

        try {
            Account acc = bank.findAccount(accNum);
            List<Transaction> statement = bank.getAccountStatement(accNum);

            System.out.println("\n" + "=".repeat(95));
            System.out.println("                           OFFICIAL ACCOUNT STATEMENT");
            System.out.println("=".repeat(95));
            System.out.printf("  Account Number : %-25s Account Holder : %s\n", acc.getAccountNumber(), acc.getAccountHolderName());
            System.out.printf("  Account Type   : %-25s Account Status : %s\n", acc.getAccountType(), acc.getStatus());
            System.out.printf("  Current Balance: Rs. %-20s Total Records  : %d\n", formatMoney(acc.getBalance()), statement.size());
            System.out.println("=".repeat(95));

            if (statement.isEmpty()) {
                printInfo("No transactions recorded for this account yet.");
                return;
            }

            String border = "+---------------------+------------+------------------+--------------------+--------------------+----------+---------------------------------+";
            System.out.println(border);
            System.out.printf("| %-19s | %-10s | %-16s | %-18s | %-18s | %-8s | %-31s |\n",
                    "Timestamp", "Txn ID", "Type", "Amount", "Balance After", "Status", "Remarks / Memo");
            System.out.println(border);

            BigDecimal totalCredits = BigDecimal.ZERO;
            BigDecimal totalDebits = BigDecimal.ZERO;

            for (Transaction t : statement) {
                boolean isCredit = t.getType() == TransactionType.DEPOSIT ||
                        t.getType() == TransactionType.TRANSFER_IN ||
                        t.getType() == TransactionType.INTEREST_CREDIT;

                if (isCredit) {
                    totalCredits = totalCredits.add(t.getAmount());
                } else {
                    totalDebits = totalDebits.add(t.getAmount());
                }

                System.out.printf("| %-19s | %-10s | %-16s | %1s Rs. %13s | Rs. %14s | %-8s | %-31s |\n",
                        t.getTimestamp().format(TS_FMT),
                        t.getTransactionId(),
                        t.getType(),
                        isCredit ? "+" : "-",
                        formatMoney(t.getAmount()),
                        formatMoney(t.getBalanceAfter()),
                        t.getStatus(),
                        truncate(t.getRemarks(), 31));
            }
            System.out.println(border);
            System.out.printf(" Summary: Total Inflow/Credits: +Rs. %s | Total Outflow/Debits: -Rs. %s | Ending Balance: Rs. %s\n",
                    formatMoney(totalCredits), formatMoney(totalDebits), formatMoney(acc.getBalance()));

        } catch (AccountNotFoundException e) {
            printError("Account not found: " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. ANALYTICS & BANK REPORTS SUB-MENU
    // =========================================================================

    private void analyticsMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n" + "=".repeat(75));
            System.out.println("                 ANALYTICS & EXECUTIVE REPORTS");
            System.out.println("=".repeat(75));
            System.out.println("  [1] Institution Executive Summary & Liquidity");
            System.out.println("  [2] Wealth Leaderboard (Top Customers by Net Worth)");
            System.out.println("  [3] Overdrawn Accounts Watchlist (Credit Monitoring)");
            System.out.println("  [0] << Return to Main Menu");
            System.out.println("-".repeat(75));

            int choice = promptInt("Enter choice [0-3]", 0, 3);
            System.out.println();

            switch (choice) {
                case 1 -> displayExecutiveSummary();
                case 2 -> displayWealthLeaderboard();
                case 3 -> displayOverdrawnAccountsWatchlist();
                case 0 -> inMenu = false;
            }
        }
    }

    private void displayExecutiveSummary() {
        printSubHeader("INDIA BANK - EXECUTIVE SUMMARY & LIQUIDITY");
        List<Customer> customers = bank.getAllCustomers();
        List<Account> accounts = bank.getAllAccounts();

        BigDecimal totalSavings = BigDecimal.ZERO;
        BigDecimal totalCurrentPositive = BigDecimal.ZERO;
        BigDecimal totalOverdraftDrawn = BigDecimal.ZERO;
        BigDecimal totalOverdraftLimitExtended = BigDecimal.ZERO;

        int savingsCount = 0;
        int currentCount = 0;
        int overdrawnCount = 0;

        for (Account a : accounts) {
            if (a instanceof SavingsAccount) {
                savingsCount++;
                totalSavings = totalSavings.add(a.getBalance());
            } else if (a instanceof CurrentAccount ca) {
                currentCount++;
                totalOverdraftLimitExtended = totalOverdraftLimitExtended.add(ca.getOverdraftLimit());
                if (ca.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                    overdrawnCount++;
                    totalOverdraftDrawn = totalOverdraftDrawn.add(ca.getBalance().abs());
                } else {
                    totalCurrentPositive = totalCurrentPositive.add(ca.getBalance());
                }
            }
        }

        BigDecimal netVaultLiquidity = totalSavings.add(totalCurrentPositive).subtract(totalOverdraftDrawn);

        System.out.println("  +-------------------------------------+-----------------------------+");
        System.out.printf("  | Total Registered Customers          | %27d |\n", customers.size());
        System.out.printf("  | Total Bank Accounts Opened          | %27d |\n", accounts.size());
        System.out.printf("  |   - Savings Accounts Count          | %27d |\n", savingsCount);
        System.out.printf("  |   - Current Accounts Count          | %27d |\n", currentCount);
        System.out.printf("  |   - Overdrawn Accounts Count        | %27d |\n", overdrawnCount);
        System.out.println("  +-------------------------------------+-----------------------------+");
        System.out.printf("  | Total Savings Deposits Balance      | Rs. %23s |\n", formatMoney(totalSavings));
        System.out.printf("  | Total Current Deposits Balance      | Rs. %23s |\n", formatMoney(totalCurrentPositive));
        System.out.printf("  | Total Overdraft Capital Utilized    | -Rs. %22s |\n", formatMoney(totalOverdraftDrawn));
        System.out.printf("  | Total Commercial Overdraft Facility | Rs. %23s |\n", formatMoney(totalOverdraftLimitExtended));
        System.out.println("  +-------------------------------------+-----------------------------+");
        System.out.printf("  | NET INSTITUTIONAL VAULT ASSETS      | Rs. %23s |\n", formatMoney(netVaultLiquidity));
        System.out.println("  +-------------------------------------+-----------------------------+");
    }

    private void displayWealthLeaderboard() {
        printSubHeader("WEALTH LEADERBOARD - TOP CUSTOMERS BY NET WORTH");
        List<Customer> customers = new ArrayList<>(bank.getAllCustomers());
        if (customers.isEmpty()) {
            printInfo("No customer records found.");
            return;
        }

        customers.sort((c1, c2) -> c2.getTotalPortfolioBalance().compareTo(c1.getTotalPortfolioBalance()));

        String border = "+------+-----------+----------------------+-----------------------------+----------+--------------------+";
        System.out.println(border);
        System.out.printf("| %-4s | %-9s | %-20s | %-27s | %-8s | %-18s |\n",
                "Rank", "Cust ID", "Customer Name", "Email", "Accounts", "Net Worth");
        System.out.println(border);

        int rank = 1;
        for (Customer c : customers) {
            System.out.printf("| %-4d | %-9s | %-20s | %-27s | %-8d | Rs. %14s |\n",
                    rank++,
                    c.getCustomerId(),
                    truncate(c.getFullName(), 20),
                    truncate(c.getEmail(), 27),
                    c.getAccounts().size(),
                    formatMoney(c.getTotalPortfolioBalance()));
            if (rank > 15) break; // Display top 15
        }
        System.out.println(border);
    }

    private void displayOverdrawnAccountsWatchlist() {
        printSubHeader("CREDIT RISK - OVERDRAWN ACCOUNTS WATCHLIST");
        List<Account> accounts = bank.getAllAccounts();
        List<CurrentAccount> overdrawn = new ArrayList<>();

        for (Account a : accounts) {
            if (a instanceof CurrentAccount ca && ca.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                overdrawn.add(ca);
            }
        }

        if (overdrawn.isEmpty()) {
            printSuccess("Healthy portfolio! No accounts are currently overdrawn.");
            return;
        }

        String border = "+-------------------+----------------------+--------------------+--------------------+--------------------+-----------------+";
        System.out.println(border);
        System.out.printf("| %-17s | %-20s | %-18s | %-18s | %-18s | %-15s |\n",
                "Account Number", "Account Holder", "Negative Balance", "Overdraft Limit", "Buffer Remaining", "Interest Fee APR");
        System.out.println(border);

        for (CurrentAccount ca : overdrawn) {
            BigDecimal remainingBuffer = ca.getTotalAvailableLiquidity();
            System.out.printf("| %-17s | %-20s | -Rs. %13s | Rs. %14s | Rs. %14s | %13s%% |\n",
                    ca.getAccountNumber(),
                    truncate(ca.getAccountHolderName(), 20),
                    formatMoney(ca.getBalance().abs()),
                    formatMoney(ca.getOverdraftLimit()),
                    formatMoney(remainingBuffer),
                    ca.getOverdraftInterestRate().multiply(new BigDecimal("100")));
        }
        System.out.println(border);
        printWarning("Found " + overdrawn.size() + " account(s) operating on commercial overdraft credit.");
    }

    // =========================================================================
    // 5. DATABASE TOOLS & SEEDER SUB-MENU
    // =========================================================================

    private void databaseToolsMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n" + "=".repeat(75));
            System.out.println("                     DATABASE & SEEDER TOOLS");
            System.out.println("=".repeat(75));
            System.out.println("  [1] View Database File & Live Table Statistics");
            System.out.println("  [2] Re-Seed Full Sample Dataset (16 Customers, 26 Accounts, 85+ Txns)");
            System.out.println("  [3] Rapid Add 5 Additional Demo Users (Append without resetting)");
            System.out.println("  [4] Reset Database Schema Completely (Wipe all data)");
            System.out.println("  [5] Execute & View Predefined Queries (queries.sql)");
            System.out.println("  [0] << Return to Main Menu");
            System.out.println("-".repeat(75));

            int choice = promptInt("Enter choice [0-5]", 0, 5);
            System.out.println();

            switch (choice) {
                case 1 -> viewDatabaseStats();
                case 2 -> {
                    if (promptYesNo("Wipe and re-seed 16 realistic customers, 26 accounts, and 85+ ledger transactions?")) {
                        DatabaseSeeder.seed(bank, true);
                        printSuccess("Database successfully re-seeded with enterprise dataset!");
                    }
                }
                case 3 -> {
                    int added = DatabaseSeeder.addMoreDemoUsers(bank, 5);
                    printSuccess("Added " + added + " new demo users with accounts to the database!");
                }
                case 4 -> {
                    if (promptYesNo("CAUTION: This will delete ALL customer, account, and transaction data! Are you sure?")) {
                        bank.resetDatabaseAndReload();
                        printSuccess("Database schema wiped and reset successfully.");
                    }
                }
                case 5 -> executePredefinedQueriesFlow();
                case 0 -> inMenu = false;
            }
        }
    }

    private void executePredefinedQueriesFlow() {
        printSubHeader("SQL RELATIONAL QUERY RUNNER (queries.sql)");
        System.out.println("Available Predefined Queries in queries.sql:");
        System.out.println("  [0]  >> Run ALL 8 Queries in Sequence");
        System.out.println("  [1]  View all customers");
        System.out.println("  [2]  View all accounts with formatted balance (Rs.)");
        System.out.println("  [3]  Total bank deposits and liquidity");
        System.out.println("  [4]  Overdrawn current accounts watchlist");
        System.out.println("  [5]  Customer portfolio breakdown");
        System.out.println("  [6]  Recent transaction ledger");
        System.out.println("  [7]  Active Customer Loan Portfolios & Monthly EMI Obligations");
        System.out.println("  [8]  Card Issuance & Security Directory by Customer & Network");
        System.out.println("  [-1] << Back to Database Tools Menu");

        int qChoice = promptInt("Select query number to execute [-1 to 8]", -1, 8);
        if (qChoice == -1) return;

        try {
            if (qChoice == 0) {
                com.bank.dao.SqlRunner.executeAndDisplayQueryFile("queries.sql", null);
            } else {
                com.bank.dao.SqlRunner.executeAndDisplayQueryFile("queries.sql", qChoice);
            }
            printSuccess("Query execution completed successfully!");
        } catch (Exception e) {
            printError("Failed to execute queries: " + e.getMessage());
        }
    }

    private void viewDatabaseStats() {
        printSubHeader("SQLITE PERSISTENCE METRICS");
        System.out.println("  JDBC Connection URL: " + DBConnection.getJdbcUrl());

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("  Live Table Counts in SQLite Database:");
            String[] tables = {"customers", "accounts", "transactions", "loans", "cards"};
            for (String tbl : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM " + tbl)) {
                    if (rs.next()) {
                        System.out.printf("    - %-20s : %d rows%n", tbl.substring(0, 1).toUpperCase() + tbl.substring(1) + " Table", rs.getInt(1));
                    }
                } catch (Exception ignored) {
                    System.out.printf("    - %-20s : (table not created)%n", tbl);
                }
            }

        } catch (Exception e) {
            printError("Failed to query database stats: " + e.getMessage());
        }
    }

    // =========================================================================
    // 6. RUN AUTOMATED TEST SUITE
    // =========================================================================

    private void runAutomatedTests() {
        printSubHeader("RUNNING BACKEND VERIFICATION & ACID TEST SUITE");
        System.out.println("Executing automated integration tests against live engine...\n");

        try {
            BankingApplication.runAutomatedVerificationSuite(bank);
            printSuccess("Automated test suite completed successfully!");
        } catch (Exception e) {
            printError("Automated test suite encountered an issue: " + e.getMessage());
        }

        promptString("\nPress [Enter] to return to the interactive menu...", true);
    }

    // =========================================================================
    // HELPER FORMATTING & PROMPT UTILITIES
    // =========================================================================

    private void printDashboardHeader() {
        List<Customer> customers = bank.getAllCustomers();
        List<Account> accounts = bank.getAllAccounts();
        BigDecimal totalBalance = accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("                     INDIA BANKING SYSTEM");
        System.out.println("=".repeat(80));
        System.out.printf("  Branch: %-15s | Customers: %-5d | Accounts: %-5d | Deposits: Rs. %s\n",
                bank.getBranchCode(), customers.size(), accounts.size(), formatMoney(totalBalance));
        System.out.println("=".repeat(80));
    }

    private void printCustomerCard(Customer c) {
        System.out.println("  +-------------------------------------------------------------+");
        System.out.printf("  | Customer ID   : %-43s |\n", c.getCustomerId());
        System.out.printf("  | Full Name     : %-43s |\n", c.getFullName());
        System.out.printf("  | Email         : %-43s |\n", c.getEmail());
        System.out.printf("  | Phone         : %-43s |\n", c.getPhoneNumber());
        System.out.printf("  | Address       : %-43s |\n", truncate(c.getAddress(), 43));
        System.out.printf("  | Total Accounts: %-43d |\n", c.getAccounts().size());
        System.out.printf("  | Net Worth     : Rs. %-39s |\n", formatMoney(c.getTotalPortfolioBalance()));
        System.out.println("  +-------------------------------------------------------------+");
    }

    private void printTransactionReceipt(String title, String txnId, java.time.LocalDateTime ts,
                                         String type, String accNum, String holder, String relatedAcc,
                                         BigDecimal amount, BigDecimal prevBal, BigDecimal newBal,
                                         String status, String remarks) {
        System.out.println("\n" + "=".repeat(75));
        System.out.printf("                    %s\n", title);
        System.out.println("=".repeat(75));
        System.out.printf("  Transaction ID   : TXN-%s\n", txnId);
        System.out.printf("  Timestamp        : %s\n", ts != null ? ts.format(TS_FMT) : "N/A");
        System.out.printf("  Transaction Type : %s\n", type);
        System.out.printf("  Account Number   : %s (%s)\n", accNum, holder);
        if (relatedAcc != null && !relatedAcc.isEmpty()) {
            System.out.printf("  Related Account  : %s\n", relatedAcc);
        }
        System.out.printf("  Amount Transacted: Rs. %s\n", formatMoney(amount));
        System.out.printf("  Previous Balance : Rs. %s\n", formatMoney(prevBal));
        System.out.printf("  New Balance      : Rs. %s\n", formatMoney(newBal));
        System.out.printf("  Execution Status : %s\n", status);
        System.out.printf("  Remarks / Memo   : %s\n", remarks);
        System.out.println("=".repeat(75));
    }

    private void printBanner(String title) {
        System.out.println("\n" + "#".repeat(80));
        System.out.println("  " + title);
        System.out.println("#".repeat(80));
    }

    private void printSubHeader(String title) {
        System.out.println("\n--- " + title + " ---");
    }

    private void printSuccess(String msg) {
        System.out.println(" [SUCCESS] " + msg);
    }

    private void printError(String msg) {
        System.out.println(" [ERROR] " + msg);
    }

    private void printWarning(String msg) {
        System.out.println(" [WARNING] " + msg);
    }

    private void printInfo(String msg) {
        System.out.println(" [INFO] " + msg);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    private boolean isCancel(String input) {
        return input == null || input.trim().equalsIgnoreCase("cancel") || input.trim().equalsIgnoreCase("0");
    }

    private String promptString(String prompt, boolean allowBlank) {
        while (true) {
            System.out.print(prompt + " > ");
            String line = scanner.nextLine();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
            if (allowBlank) {
                return "";
            }
            System.out.println("  Input cannot be empty. Please enter a valid value (or 'cancel').");
        }
    }

    private String promptNonBlank(String prompt) {
        return promptString(prompt, false);
    }

    private String promptEmail(String prompt) {
        while (true) {
            String email = promptNonBlank(prompt);
            if (isCancel(email)) return email;
            if (email.contains("@") && email.contains(".")) {
                return email;
            }
            System.out.println("  Invalid email format. Must contain '@' and '.' (or type 'cancel').");
        }
    }

    private int promptInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " > ");
            String line = scanner.nextLine();
            if (line == null) continue;
            try {
                int val = Integer.parseInt(line.trim());
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("  Please enter a number between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.printf("  Invalid numeric format. Please enter a number between %d and %d.\n", min, max);
            }
        }
    }

    private BigDecimal promptBigDecimal(String prompt, BigDecimal min, BigDecimal max) {
        while (true) {
            System.out.print(prompt + " > ");
            String line = scanner.nextLine();
            if (line == null) continue;
            String trimmed = line.trim().replace("₹", "").replace("Rs.", "").replace("Rs", "").replace("$", "").replace(",", "").trim();
            if (trimmed.equalsIgnoreCase("cancel")) {
                return null;
            }
            try {
                BigDecimal val = new BigDecimal(trimmed).setScale(2, RoundingMode.HALF_UP);
                if (min != null && val.compareTo(min) < 0) {
                    System.out.println("  Amount cannot be less than Rs. " + formatMoney(min) + ".");
                    continue;
                }
                if (max != null && val.compareTo(max) > 0) {
                    System.out.println("  Amount cannot exceed Rs. " + formatMoney(max) + ".");
                    continue;
                }
                return val;
            } catch (Exception e) {
                System.out.println("  Invalid currency/decimal input. Example: 150.00 (or type 'cancel').");
            }
        }
    }

    private boolean promptYesNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n) > ");
            String line = scanner.nextLine();
            if (line == null) continue;
            String t = line.trim().toLowerCase();
            if (t.equals("y") || t.equals("yes")) return true;
            if (t.equals("n") || t.equals("no")) return false;
            System.out.println("  Please enter 'y' for yes or 'n' for no.");
        }
    }
}
