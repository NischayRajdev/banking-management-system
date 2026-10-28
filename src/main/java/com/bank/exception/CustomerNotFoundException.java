package com.bank.exception;

/**
 * Thrown when a customer lookup fails by ID or search criteria.
 */
public class CustomerNotFoundException extends BankingException {

    private static final long serialVersionUID = 1L;
    private final String customerIdentifier;

    public CustomerNotFoundException(String customerIdentifier) {
        super("ERR_CUSTOMER_NOT_FOUND", String.format("Customer with identifier '%s' was not found.", customerIdentifier));
        this.customerIdentifier = customerIdentifier;
    }

    public String getCustomerIdentifier() {
        return customerIdentifier;
    }
}

