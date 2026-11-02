-- =====================================================================
-- India Banking Management System - Master Analytical & Operational Queries
-- =====================================================================

-- 1. View all customers
SELECT customer_id, first_name, last_name, email, phone, address 
FROM customers;

-- 2. View all accounts with holder name and balance in Rupees
SELECT account_number, account_holder_name, account_type, 'Rs. ' || balance AS formatted_balance, status 
FROM accounts 
ORDER BY balance DESC;

-- 3. Total bank deposits and liquidity in Rupees
SELECT 
    COUNT(*) AS total_accounts,
    'Rs. ' || SUM(balance) AS total_deposits_inr,
    'Rs. ' || ROUND(AVG(balance), 2) AS average_balance_inr,
    'Rs. ' || MAX(balance) AS highest_balance_inr
FROM accounts;

-- 4. Overdrawn current accounts
SELECT account_number, account_holder_name, balance, overdraft_limit,
       (overdraft_limit + balance) AS available_liquidity
FROM accounts 
WHERE balance < 0;

-- 5. Customer portfolio breakdown
SELECT c.customer_id, c.first_name || ' ' || c.last_name AS customer_name,
       COUNT(a.account_number) AS account_count,
       'Rs. ' || COALESCE(SUM(a.balance), 0) AS total_portfolio_balance_inr
FROM customers c
LEFT JOIN accounts a ON c.customer_id = a.customer_id
GROUP BY c.customer_id
ORDER BY SUM(a.balance) DESC;

-- 6. Recent transaction ledger
SELECT t.transaction_id, t.timestamp, t.type, t.account_number, 
       'Rs. ' || t.amount AS amount_inr, 'Rs. ' || t.balance_after AS balance_after_inr, 
       t.status, t.remarks
FROM transactions t
ORDER BY t.timestamp DESC 
LIMIT 20;

-- 7. Active Customer Loan Portfolios & Monthly EMI Obligations
SELECT l.loan_id, 
       c.first_name || ' ' || c.last_name AS customer_name,
       l.loan_type,
       'Rs. ' || l.principal_amount AS principal_amount_inr,
       ROUND(l.interest_rate * 100, 2) || '%' AS interest_rate_apr,
       l.term_months || ' Months' AS tenure,
       'Rs. ' || l.monthly_emi AS monthly_emi_inr,
       'Rs. ' || l.outstanding_balance AS outstanding_balance_inr,
       l.status
FROM loans l
JOIN customers c ON l.customer_id = c.customer_id
WHERE l.status = 'ACTIVE'
ORDER BY l.outstanding_balance DESC;

-- 8. Card Issuance & Security Directory by Customer & Network
SELECT crd.card_id,
       c.first_name || ' ' || c.last_name AS cardholder_name,
       crd.card_number,
       crd.card_type,
       crd.card_network,
       crd.expiry_date,
       'Rs. ' || crd.daily_limit AS daily_spend_limit_inr,
       CASE 
           WHEN crd.credit_limit IS NOT NULL THEN 'Rs. ' || crd.credit_limit 
           ELSE 'N/A (Debit Linked)' 
       END AS credit_limit_inr,
       crd.status
FROM cards crd
JOIN customers c ON crd.customer_id = c.customer_id
ORDER BY crd.card_type, crd.card_network;
