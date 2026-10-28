

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

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_number);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);

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
