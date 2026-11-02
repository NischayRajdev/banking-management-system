package com.bank.dao;

import com.bank.exception.DatabaseException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Properties;

// DBConnection: Singleton JDBC connection manager for SQLite
public class DBConnection {

    private static final String DEFAULT_URL = "jdbc:sqlite:bank_system.db";
    private static final String DEFAULT_DRIVER = "org.sqlite.JDBC";

    private static String jdbcUrl;
    private static String jdbcUser;
    private static String jdbcPassword;
    private static boolean initialized = false;

    static {
        loadConfiguration();
    }

    private DBConnection() {
    }

    // main: Creates and initializes database schema directly
    public static void main(String[] args) {
        initializeDatabase();
        System.out.println("Database schema successfully created at: " + getJdbcUrl());
    }

    // loadConfiguration: Loads database properties and registers SQLite driver
    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
                jdbcUrl = props.getProperty("db.url", DEFAULT_URL);
                jdbcUser = props.getProperty("db.user", "");
                jdbcPassword = props.getProperty("db.password", "");
                String driverClass = props.getProperty("db.driver", DEFAULT_DRIVER);
                Class.forName(driverClass);
            } else {
                jdbcUrl = DEFAULT_URL;
                jdbcUser = "";
                jdbcPassword = "";
                Class.forName(DEFAULT_DRIVER);
            }
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("JDBC Driver not found. Please ensure sqlite-jdbc jar is on the classpath.", e);
        } catch (Exception e) {
            System.err.println("Warning: Could not read db.properties, using fallback configuration: " + e.getMessage());
            jdbcUrl = DEFAULT_URL;
            jdbcUser = "";
            jdbcPassword = "";
        }
    }

    // getConnection: Returns active JDBC database connection
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            synchronized (DBConnection.class) {
                if (!initialized) {
                    initializeDatabase();
                    initialized = true;
                }
            }
        }
        if (jdbcUser != null && !jdbcUser.isEmpty()) {
            return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    // initializeDatabase: Creates customers, accounts, transactions, and auxiliary banking tables if not existing
    public static synchronized void initializeDatabase() {
        String createCustomersTable = """
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id VARCHAR(50) PRIMARY KEY,
                    first_name VARCHAR(100) NOT NULL,
                    last_name VARCHAR(100) NOT NULL,
                    email VARCHAR(150) UNIQUE NOT NULL,
                    phone VARCHAR(50),
                    address VARCHAR(255),
                    created_at VARCHAR(50) NOT NULL
                );
                """;

        String createAccountsTable = """
                CREATE TABLE IF NOT EXISTS accounts (
                    account_number VARCHAR(50) PRIMARY KEY,
                    customer_id VARCHAR(50) NOT NULL,
                    account_holder_name VARCHAR(150) NOT NULL,
                    balance DECIMAL(15,2) NOT NULL,
                    account_type VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    interest_rate DECIMAL(6,4),
                    minimum_balance DECIMAL(15,2),
                    max_withdrawal_limit DECIMAL(15,2),
                    overdraft_limit DECIMAL(15,2),
                    overdraft_interest_rate DECIMAL(6,4),
                    created_at VARCHAR(50) NOT NULL,
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                );
                """;

        String createTransactionsTable = """
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id VARCHAR(50) PRIMARY KEY,
                    account_number VARCHAR(50) NOT NULL,
                    related_account_number VARCHAR(50),
                    type VARCHAR(30) NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    balance_after DECIMAL(15,2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    remarks VARCHAR(255),
                    timestamp VARCHAR(50) NOT NULL,
                    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
                );
                """;

        String createLoansTable = """
                CREATE TABLE IF NOT EXISTS loans (
                    loan_id VARCHAR(50) PRIMARY KEY,
                    customer_id VARCHAR(50) NOT NULL,
                    account_number VARCHAR(50),
                    loan_type VARCHAR(30) NOT NULL,
                    principal_amount DECIMAL(15,2) NOT NULL,
                    interest_rate DECIMAL(6,4) NOT NULL,
                    term_months INTEGER NOT NULL,
                    monthly_emi DECIMAL(15,2) NOT NULL,
                    outstanding_balance DECIMAL(15,2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    disbursed_at VARCHAR(50) NOT NULL,
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
                );
                """;

        String createCardsTable = """
                CREATE TABLE IF NOT EXISTS cards (
                    card_id VARCHAR(50) PRIMARY KEY,
                    account_number VARCHAR(50) NOT NULL,
                    customer_id VARCHAR(50) NOT NULL,
                    card_number VARCHAR(30) UNIQUE NOT NULL,
                    card_type VARCHAR(20) NOT NULL,
                    card_network VARCHAR(20) NOT NULL,
                    expiry_date VARCHAR(10) NOT NULL,
                    daily_limit DECIMAL(15,2) NOT NULL,
                    credit_limit DECIMAL(15,2),
                    status VARCHAR(20) NOT NULL,
                    issued_at VARCHAR(50) NOT NULL,
                    FOREIGN KEY (account_number) REFERENCES accounts(account_number),
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                );
                """;

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createCustomersTable);
            stmt.execute(createAccountsTable);
            stmt.execute(createTransactionsTable);
            stmt.execute(createLoansTable);
            stmt.execute(createCardsTable);

            // Create performance indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(account_type);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_number);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_loans_customer ON loans(customer_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cards_account ON cards(account_number);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cards_customer ON cards(customer_id);");

        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database schema.", e);
        }
    }

    // resetDatabase: Drops all tables and recreates clean schema
    public static synchronized void resetDatabase() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS cards;");
            stmt.execute("DROP TABLE IF EXISTS loans;");
            stmt.execute("DROP TABLE IF EXISTS transactions;");
            stmt.execute("DROP TABLE IF EXISTS accounts;");
            stmt.execute("DROP TABLE IF EXISTS customers;");
            initialized = false;
            initializeDatabase();
            initialized = true;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to reset database.", e);
        }
    }

    // setJdbcUrl: Configures custom database connection URL
    public static synchronized void setJdbcUrl(String newUrl) {
        jdbcUrl = newUrl;
        initialized = false;
    }

    // getJdbcUrl: Returns currently configured database connection URL
    public static String getJdbcUrl() {
        return jdbcUrl;
    }

    // parseDateTime: Robust parser for ISO, standard SQL, and date-only timestamp formats
    public static LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        String s = raw.trim().replace(' ', 'T');
        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1);
        }
        if (!s.contains("T")) {
            s += "T00:00:00";
        }
        return LocalDateTime.parse(s);
    }
}
