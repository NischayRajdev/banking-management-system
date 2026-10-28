

CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address VARCHAR(255),
    created_at VARCHAR(50) NOT NULL
);

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

CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_number);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp);
