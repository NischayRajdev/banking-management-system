package com.bank.dao;

import com.bank.exception.DatabaseException;
import com.bank.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of CustomerDAO using PreparedStatement to ensure
 * protection against SQL Injection attacks.
 */
public class CustomerDAOImpl implements CustomerDAO {

    @Override
    public void save(Customer customer) {
        String sql = """
                INSERT INTO customers (customer_id, first_name, last_name, email, phone, address, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getCustomerId());
            ps.setString(2, customer.getFirstName());
            ps.setString(3, customer.getLastName());
            ps.setString(4, customer.getEmail());
            ps.setString(5, customer.getPhoneNumber());
            ps.setString(6, customer.getAddress());
            ps.setString(7, customer.getCreatedAt().toString());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to save customer: " + customer.getCustomerId(), e);
        }
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        String sql = "SELECT * FROM customers WHERE customer_id = ?;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error finding customer by ID: " + customerId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        String sql = "SELECT * FROM customers WHERE LOWER(email) = LOWER(?);";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error finding customer by email: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT * FROM customers ORDER BY created_at DESC;";
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all customers.", e);
        }
        return customers;
    }

    @Override
    public void update(Customer customer) {
        String sql = """
                UPDATE customers
                SET first_name = ?, last_name = ?, email = ?, phone = ?, address = ?
                WHERE customer_id = ?;
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getFirstName());
            ps.setString(2, customer.getLastName());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getPhoneNumber());
            ps.setString(5, customer.getAddress());
            ps.setString(6, customer.getCustomerId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update customer: " + customer.getCustomerId(), e);
        }
    }

    @Override
    public void delete(String customerId) {
        String sql = "DELETE FROM customers WHERE customer_id = ?;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete customer: " + customerId, e);
        }
    }

    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        String customerId = rs.getString("customer_id");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String address = rs.getString("address");
        String createdAtStr = rs.getString("created_at");
        LocalDateTime createdAt = DBConnection.parseDateTime(createdAtStr);

        return new Customer(customerId, firstName, lastName, email, phone, address, createdAt);
    }
}

