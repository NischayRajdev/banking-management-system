# Production-Grade Banking Management System (India Bank)

A robust, enterprise-grade Banking Management System developed in Java 21 LTS with an interactive console terminal UI and SQLite JDBC persistence, denominated in Indian Rupees (INR / Rs.). This system adheres strictly to Object-Oriented Programming (OOP) principles, clean architecture patterns, defensive domain validation, and ACID-compliant relational transactions.

---

## 1. Features Overview

### 🎮 Interactive Console Terminal UI
- **Input-Driven Workflows**: Real-time user input for customer onboarding, account creation, deposits, withdrawals, fund transfers, and ledger queries in Indian Rupees (Rs.).
- **Instant Financial Receipts**: Formatted transaction receipts with unique transaction IDs, precise timestamps, previous vs. updated balances (Rs.), and audit memos.
- **Visual Tables & Reports**: Tabular ASCII views for Customer Directory, Account Listings, Chronological Account Statements, Wealth Leaderboards, and Credit Risk Monitoring.
- **Defensive Error Handling**: Safe input validation prevents crashes on invalid numbers, negative amounts, or malformed emails. Users can type `cancel` or `0` at any sub-prompt to safely abort.

### 👥 Expanded Enterprise Database (Pre-Seeded)
- **16+ Realistic Indian Customers**: Diverse profiles across tech, medical, real estate, hospitality, legal, and education sectors across Mumbai, Bengaluru, New Delhi, Chennai, Kolkata, Hyderabad, and Pune.
- **26+ Savings & Current Accounts**:
  - High-Yield Savings Accounts (up to 5.25% APY)
  - Premier & Standard Savings Accounts
  - Commercial Current Accounts with Overdraft facilities (Rs. 20,000 to Rs. 3,00,000 limit)
  - Pre-configured overdrawn accounts showcasing live overdraft credit utilization and interest fee mechanics.
- **85+ Chronological Ledger Transactions**: Direct payroll deposits, vendor invoices, utility bills, UPI/NEFT transfers, card settlements, interest credits, and overdraft charges spanning 60 days of history.
- **Auto-Seeding**: When started with an empty database, the system automatically seeds this rich dataset on first boot. Additional users can be added on the fly via the Database Tools menu.

---

## 2. Architecture & Package Structure

```
banking-management-system/
├── lib/                                     # Bundled JDBC driver & SLF4J dependencies
│   ├── sqlite-jdbc.jar
│   ├── slf4j-api.jar
│   └── slf4j-simple.jar
├── pom.xml                                  # Maven project configuration
├── run.bat                                  # Windows CMD build and run script
├── run.ps1                                  # Windows PowerShell build and run script
├── bank_system.db                           # Embedded SQLite Relational Database
├── README.md                                # System architectural documentation
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── bank/
        │           ├── model/               # Core Domain Entities & Interfaces
        │           │   ├── Account.java                (Abstract Base Account)
        │           │   ├── SavingsAccount.java         (Concrete Savings Account)
        │           │   ├── CurrentAccount.java         (Concrete Current Account)
        │           │   ├── Customer.java               (Customer Entity with 1:N Accounts)
        │           │   ├── Transaction.java            (Immutable Financial Ledger Entry)
        │           │   ├── Transactable.java           (Interface for Debit/Credit behavior)
        │           │   ├── Auditable.java              (Interface for Compliance & Auditing)
        │           │   ├── AccountType.java            (Enum: SAVINGS, CURRENT)
        │           │   ├── AccountStatus.java          (Enum: ACTIVE, FROZEN, CLOSED)
        │           │   ├── TransactionType.java        (Enum: DEPOSIT, WITHDRAWAL, TRANSFER_*, etc.)
        │           │   └── TransactionStatus.java      (Enum: SUCCESS, FAILED, REVERSED)
        │           │
        │           ├── exception/           # Custom Exception Hierarchy
        │           │   ├── BankingException.java            (Base Checked Exception)
        │           │   ├── InsufficientBalanceException.java(Checked: Low balance / min balance breach)
        │           │   ├── OverdraftExceededException.java  (Checked: Current account overdraft breach)
        │           │   ├── AccountNotFoundException.java    (Checked: Account lookup failure)
        │           │   ├── CustomerNotFoundException.java   (Checked: Customer lookup failure)
        │           │   ├── InvalidTransactionException.java (Checked: Constraint/limit violations)
        │           │   └── DatabaseException.java           (Unchecked: Persistence/SQL errors)
        │           │
        │           ├── dao/                 # JDBC Persistence Layer (DAO Pattern)
        │           │   ├── DBConnection.java                (Singleton JDBC Connection & DDL Manager)
        │           │   ├── CustomerDAO.java                 (Customer persistence contract)
        │           │   ├── CustomerDAOImpl.java             (PreparedStatement SQL implementation)
        │           │   ├── AccountDAO.java                  (Account persistence contract)
        │           │   ├── AccountDAOImpl.java              (Polymorphic PreparedStatement persistence)
        │           │   ├── TransactionDAO.java              (Ledger persistence contract)
        │           │   └── TransactionDAOImpl.java          (PreparedStatement transaction ledger)
        │           │
        │           ├── service/             # Business Logic & Operational Orchestration
        │           │   ├── BankService.java                 (Service contract interface)
        │           │   ├── Bank.java                        (Central Service Controller & ACID Facade)
        │           │   └── DatabaseSeeder.java              (Enterprise relational mock data generator)
        │           │
        │           └── main/                # Interactive Console UI & Test Runner
        │               ├── BankingApplication.java          (Application Entry Point)
        │               └── InteractiveBankingCLI.java       (Interactive Menu-Driven Console Terminal)
        │
        └── resources/
            └── db.properties                # Database connection configuration
```

---

## 3. Interactive Menu Navigation

When running the application, users are presented with the main navigation menu:

```
================================================================================
                 INDIA BANKING MANAGEMENT SYSTEM - CORE CLI
================================================================================
  Branch: INDB-001        | Customers: 18    | Accounts: 30    | Deposits: Rs. 78,69,000.00
================================================================================
>>> MAIN NAVIGATION MENU <<<
  [1] Customer Management       (Register, View All, Search, Portfolio, Update)
  [2] Account Management        (Open Savings/Current, View All, Filter, Status)
  [3] Financial Transactions    (Deposit, Withdraw, Transfer, Interest, Statement)
  [4] Analytics & Bank Reports  (Vault Summary, Wealth Ranking, Overdrawn List)
  [5] Database & Seeder Tools   (Seed Demo Data, Add More Users, Reset DB)
  [6] Run Automated Test Suite  (Execute OOP & ACID Verification Tests)
  [0] Exit Application
```

### 1. Customer Management
- **Register New Customer**: Prompts for demographics (First/Last Name, Email, Phone, Address), validates email format, and immediately offers to open a bank account.
- **View All Customers**: Displays formatted directory with customer ID, email, telephone, associated account count, and aggregate portfolio net worth.
- **Search Customer**: Finds customers by ID, email, or full name substring.
- **View Customer Portfolio**: Shows full profile and a detailed table of all accounts owned.
- **Update Customer Contact**: Safely updates phone number or address in database.

### 2. Account Management
- **Open Savings Account**: Prompts for Customer ID, initial deposit (validating against minimum balance), interest rate, and withdrawal limits.
- **Open Current (Checking) Account**: Prompts for Customer ID, initial deposit, custom overdraft limit, and overdraft APR fee rate.
- **View All Accounts**: Renders institution-wide directory of account numbers, holder names, types, balances, and available liquidity.
- **View Account Details**: Dynamic polymorphic output showing account-specific rules and constraints.
- **Filter Accounts by Balance Range**: Searches accounts between custom minimum and maximum bounds.
- **Modify Account Status**: Updates status (`ACTIVE`, `FROZEN`, `CLOSED`).

### 3. Financial Transactions (Input -> Output)
- **Deposit Funds**: Prompts for Account Number, Amount, and optional memo; outputs formatted receipt and updates balance.
- **Withdraw Funds**: Enforces minimum balances (Savings) or overdraft limits (Current); outputs receipt or clear rejection notice.
- **Fund Transfer (ACID)**: Prompts for Sender, Recipient, Amount, and Memo. Prompts user confirmation `(y/n)` before executing atomic transfer; displays two-legged ledger audit receipt.
- **Post Interest / Overdraft Fee**: Credits annual interest to Savings Accounts or bills overdraft fee on overdrawn Current Accounts.
- **Account Ledger Statement**: Displays chronological ledger table of all transactions with running balance after each entry.

### 4. Analytics & Executive Reports
- **Executive Summary & Liquidity**: Total deposits, total commercial overdraft deployed, net institution vault assets.
- **Wealth Leaderboard**: Top customers ranked by total net worth.
- **Overdrawn Watchlist**: Monitors accounts operating on commercial overdraft credit.

### 5. Database & Seeder Tools
- **Live Table Statistics**: Row counts for `customers`, `accounts`, and `transactions`.
- **Re-Seed Full Dataset**: Re-initializes clean schema with 16 customers, 26 accounts, and 85+ transactions.
- **Add 5 Additional Demo Users**: Injects new randomized customers and accounts without resetting existing data.
- **Reset Database**: Wipes schema for a completely fresh start.

### 6. Automated Test Suite
- Runs the backend engine verification tests (Encapsulation, Inheritance, Polymorphism, ACID rollback, Persistence) on demand.

---

## 4. How to Build & Run

### Prerequisites
- Java JDK 21 (or 17+) installed and available in `PATH`.

### Option 1: Using the provided Batch script (Windows CMD)
```cmd
.\run.bat
```
To run the automated verification suite directly:
```cmd
.\run.bat --test
```
To re-seed the enterprise dataset on startup:
```cmd
.\run.bat --reseed
```

### Option 2: Using the provided PowerShell script
```powershell
.\run.ps1
```
Or with arguments:
```powershell
.\run.ps1 --test
.\run.ps1 --reseed
```

### Option 3: Using Maven
```cmd
mvn clean compile exec:java
```

### Option 4: Manual Compilation with standard `javac` & `java`
```cmd
javac -d bin -cp "lib/*" src/main/java/com/bank/model/*.java src/main/java/com/bank/exception/*.java src/main/java/com/bank/dao/*.java src/main/java/com/bank/service/*.java src/main/java/com/bank/main/*.java src/main/java/com/bank/api/*.java
copy /y src\main\resources\* bin\
java -cp "bin;lib/*" com.bank.main.BankingApplication
```

---

## 5. SQL Database Schema & Query Runners (`queries.sql`)

The database includes 5 core relational tables with 3NF foreign-key relationships and performance indexes:
1. `customers`: Customer demographic and contact records
2. `accounts`: Polymorphic Savings and Current accounts with balances and overdraft limits
3. `transactions`: ACID financial ledger audit history
4. `loans`: Loan accounts (Personal, Home, Auto, Business) with EMI schedules and balances
5. `cards`: Debit and Credit cards with network issuers (RuPay, Visa, Mastercard) and daily limits

### Running Queries (`queries.sql`)

You can execute all 8 predefined business queries or a specific query using any of the following connections:

#### Option A: Using PowerShell Query Runner
```powershell
# Run all 8 queries with formatted tabular results
.\run-queries.ps1

# Run a specific query by number (e.g., Query #7: Loan Portfolios)
.\run-queries.ps1 7
```

#### Option B: Using Windows Command Prompt (CMD)
```cmd
# Run all queries
.\run-queries.bat

# Run specific query
.\run-queries.bat 7
```

#### Option C: Using Java JDBC Directly
```cmd
java -cp "bin;lib/*" com.bank.dao.SqlRunner queries.sql
java -cp "bin;lib/*" com.bank.dao.SqlRunner queries.sql 8
```

#### Option D: Using Fast Node.js Runner (`node:sqlite`)
```cmd
node run-queries.cjs
node run-queries.cjs 9
```

#### Option E: From the Interactive Console Menu
Start the application (`.\run.ps1` or `.\run.bat`), select:
`[5] Database & Seeder Tools` -> `[5] Execute & View Predefined Queries (queries.sql)`

#### Option F: Via REST API
When the server is running (`.\start.ps1` or `.\start.bat`):
- List available queries: `GET http://localhost:8080/api/queries`
- Run query by ID: `GET http://localhost:8080/api/queries/run?id=7`
- View table statistics: `GET http://localhost:8080/api/database/tables`

---

## 6. Modern Web Dashboard (Single Localhost: `http://localhost:8080`)

The application includes a React 18, TypeScript, Tailwind CSS, and Recharts single-page dashboard served directly alongside the Java backend on a single localhost URL.

### Launch Unified Web & API Server
Run either script from the project root:

**PowerShell:**
```powershell
.\start.ps1
```

**Windows CMD / Batch:**
```cmd
start.bat
```

Once started, open your web browser at:
```
http://localhost:8080
```

- **Frontend Dashboard**: `http://localhost:8080/`
- **Core REST API**: `http://localhost:8080/api`
- **API Health Check**: `http://localhost:8080/api/health`
