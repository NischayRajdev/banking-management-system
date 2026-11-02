-- =====================================================================
-- India Banking Management System - Master Database Schema
-- SQLite Relational DDL & Seed Dataset (Denominated in Indian Rupees Rs.)
-- =====================================================================

-- 1. Customers Table
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address VARCHAR(255),
    created_at VARCHAR(50) NOT NULL
);

-- 2. Accounts Table
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

-- 3. Transactions Table
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

-- 4. Loans Table (Personal, Home, Auto, & Commercial Facilities)
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

-- 5. Cards Table (Debit & Credit Cards with Network & Security Limits)
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

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_number);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);
CREATE INDEX IF NOT EXISTS idx_loans_customer ON loans(customer_id);
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);
CREATE INDEX IF NOT EXISTS idx_cards_account ON cards(account_number);
CREATE INDEX IF NOT EXISTS idx_cards_customer ON cards(customer_id);

-- Initial Seed Customers (Indian Names & Metros)
INSERT OR IGNORE INTO customers (customer_id, first_name, last_name, email, phone, address, created_at) VALUES
('CUST-1001', 'Aarav', 'Sharma', 'aarav.sharma@example.com', '+91-98201-10001', '123 Marine Drive, Mumbai, MH', datetime('now')),
('CUST-1002', 'Priya', 'Patel', 'priya.patel@example.com', '+91-98201-10002', '456 Brigade Road, Bengaluru, KA', datetime('now')),
('CUST-1003', 'Rohit', 'Verma', 'rohit.verma@example.com', '+91-98201-10003', '789 Connaught Place, New Delhi, DL', datetime('now')),
('CUST-1004', 'Ananya', 'Iyer', 'ananya.iyer@example.com', '+91-98201-10004', '12 Anna Salai, Chennai, TN', datetime('now'));

-- Initial Seed Accounts (Values in Indian Rupees Rs.)
INSERT OR IGNORE INTO accounts (account_number, customer_id, account_holder_name, balance, account_type, status, interest_rate, minimum_balance, max_withdrawal_limit, overdraft_limit, overdraft_interest_rate, created_at) VALUES
('SAV-INDB-001-1001', 'CUST-1001', 'Aarav Sharma', 125000.00, 'SAVINGS', 'ACTIVE', 0.0450, 5000.00, 50000.00, NULL, NULL, datetime('now')),
('CUR-INDB-001-1002', 'CUST-1001', 'Aarav Sharma', 54000.00, 'CURRENT', 'ACTIVE', NULL, NULL, NULL, 50000.00, 0.0950, datetime('now')),
('CUR-INDB-001-1003', 'CUST-1002', 'Priya Patel', 32000.00, 'CURRENT', 'ACTIVE', NULL, NULL, NULL, 30000.00, 0.1000, datetime('now')),
('SAV-INDB-001-1004', 'CUST-1003', 'Rohit Verma', 285000.00, 'SAVINGS', 'ACTIVE', 0.0525, 10000.00, 75000.00, NULL, NULL, datetime('now')),
('SAV-INDB-001-1005', 'CUST-1004', 'Ananya Iyer', 89000.00, 'SAVINGS', 'ACTIVE', 0.0400, 5000.00, 30000.00, NULL, NULL, datetime('now'));

-- Initial Seed Loans
INSERT OR IGNORE INTO loans (loan_id, customer_id, account_number, loan_type, principal_amount, interest_rate, term_months, monthly_emi, outstanding_balance, status, disbursed_at) VALUES
('LN-1001', 'CUST-1001', 'SAV-INDB-001-1001', 'HOME_LOAN', 4500000.00, 0.0825, 240, 38320.00, 4120000.00, 'ACTIVE', '2024-03-15 10:30:00'),
('LN-1002', 'CUST-1002', 'CUR-INDB-001-1003', 'BUSINESS_LOAN', 1200000.00, 0.1100, 48, 31050.00, 890000.00, 'ACTIVE', '2024-07-20 11:15:00'),
('LN-1003', 'CUST-1003', 'SAV-INDB-001-1004', 'VEHICLE_LOAN', 850000.00, 0.0910, 60, 17680.00, 580000.00, 'ACTIVE', '2024-11-10 14:00:00'),
('LN-1004', 'CUST-1004', 'SAV-INDB-001-1005', 'PERSONAL_LOAN', 300000.00, 0.1250, 24, 14190.00, 145000.00, 'ACTIVE', '2025-02-18 09:45:00');

-- Initial Seed Cards (Debit & Credit)
INSERT OR IGNORE INTO cards (card_id, account_number, customer_id, card_number, card_type, card_network, expiry_date, daily_limit, credit_limit, status, issued_at) VALUES
('CRD-1001', 'SAV-INDB-001-1001', 'CUST-1001', '4532-XXXX-XXXX-9821', 'DEBIT', 'VISA', '08/29', 50000.00, NULL, 'ACTIVE', '2024-01-15 10:00:00'),
('CRD-1002', 'CUR-INDB-001-1002', 'CUST-1001', '5241-XXXX-XXXX-1144', 'CREDIT', 'MASTERCARD', '11/28', 100000.00, 300000.00, 'ACTIVE', '2024-02-01 11:30:00'),
('CRD-1003', 'CUR-INDB-001-1003', 'CUST-1002', '6071-XXXX-XXXX-3312', 'DEBIT', 'RUPAY', '05/30', 40000.00, NULL, 'ACTIVE', '2024-04-10 09:15:00'),
('CRD-1004', 'SAV-INDB-001-1004', 'CUST-1003', '4111-XXXX-XXXX-7789', 'CREDIT', 'VISA', '09/27', 150000.00, 500000.00, 'ACTIVE', '2024-05-18 14:20:00'),
('CRD-1005', 'SAV-INDB-001-1005', 'CUST-1004', '6521-XXXX-XXXX-8823', 'DEBIT', 'RUPAY', '01/30', 30000.00, NULL, 'ACTIVE', '2024-07-01 11:00:00');
