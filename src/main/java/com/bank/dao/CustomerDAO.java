package com.bank.dao;

import com.bank.model.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Customer persistence operations.
 */
public interface CustomerDAO {

    /**
     * Persists a new customer record into the database.
     */
    void save(Customer customer);

    /**
     * Finds a customer by primary key (Customer ID).
     */
    Optional<Customer> findById(String customerId);

    /**
     * Finds a customer by unique email address.
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Retrieves all customer profiles.
     */
    List<Customer> findAll();

    /**
     * Updates an existing customer profile.
     */
    void update(Customer customer);

    /**
     * Deletes a customer by ID.
     */
    void delete(String customerId);
}

