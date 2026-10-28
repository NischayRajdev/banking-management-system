package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankingException;
import com.bank.exception.CustomerNotFoundException;
import com.bank.model.Account;
import com.bank.model.CurrentAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface specifying all banking operations.
 */
public interface BankService {

    // Customer operations
    Customer registerCustomer(String firstName, String lastName, String email, String phone, String address) throws BankingException;

    // Account creation operations
    SavingsAccount openSavingsAccount(String customerId, BigDecimal initialDeposit) throws BankingException;
    SavingsAccount openSavingsAccount(String customerId, BigDecimal initialDeposit, BigDecimal interestRate, BigDecimal minBalance, BigDecimal maxWithdrawal) throws BankingException;

    CurrentAccount openCurrentAccount(String customerId, BigDecimal initialDeposit) throws BankingException;
    CurrentAccount openCurrentAccount(String customerId, BigDecimal initialDeposit, BigDecimal overdraftLimit, BigDecimal overdraftInterestRate) throws BankingException;

    // Transactional operations (Overloading Polymorphism)
    Transaction deposit(String accountNumber, BigDecimal amount) throws BankingException;
    Transaction deposit(String accountNumber, BigDecimal amount, String remarks) throws BankingException;

    Transaction withdraw(String accountNumber, BigDecimal amount) throws BankingException;
    Transaction withdraw(String accountNumber, BigDecimal amount, String remarks) throws BankingException;

    List<Transaction> transfer(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount) throws BankingException;
    List<Transaction> transfer(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount, String remarks) throws BankingException;

    // Balance and Interest calculations
    BigDecimal checkBalance(String accountNumber) throws AccountNotFoundException;
    Transaction applyInterest(String accountNumber) throws BankingException;

    // Query & Search operations (Overloading Polymorphism)
    Account findAccount(String accountNumber) throws AccountNotFoundException;
    List<Account> findAccountsByCustomer(String customerId) throws CustomerNotFoundException;
    List<Account> findAccountsByBalanceRange(BigDecimal minBalance, BigDecimal maxBalance);

    Customer findCustomer(String customerId) throws CustomerNotFoundException;
    Customer findCustomerByEmail(String email) throws CustomerNotFoundException;

    List<Customer> getAllCustomers();
    List<Account> getAllAccounts();
    List<Transaction> getAccountStatement(String accountNumber) throws AccountNotFoundException;

    // Customer & Account management extensions
    Customer updateCustomer(String customerId, String firstName, String lastName, String phone, String address) throws CustomerNotFoundException;
    Account updateAccountStatus(String accountNumber, com.bank.model.AccountStatus status) throws AccountNotFoundException;
    List<Customer> searchCustomers(String query);
}

