package com.bank.model;

import java.time.LocalDateTime;

/**
 * Interface representing entities that require an audit trail in compliance
 * with standard banking regulatory frameworks.
 */
public interface Auditable {

    /**
     * Unique identifier used for audit indexing (e.g., Account Number, Customer ID, Transaction ID).
     */
    String getAuditId();

    /**
     * Timestamp at which the entity or record was created.
     */
    LocalDateTime getCreatedAt();

    /**
     * Brief summary string captured for logging and audit ledgers.
     */
    String getAuditSummary();
}

