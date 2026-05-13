package com.bankcore.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a state change on any domain entity for the immutable audit log.
 * Published by every service that mutates persistent state.
 *
 * <p><strong>Topic:</strong> {@code audit.event}
 * <br><strong>Consumer:</strong> {@code audit-service} (append-only writes to {@code audit_logs})
 *
 * <h2>Entity types (examples)</h2>
 * {@code "ACCOUNT"}, {@code "TRANSACTION"}, {@code "CARD"}, {@code "USER"}
 *
 * <h2>Actions (examples)</h2>
 * {@code "CREATED"}, {@code "UPDATED"}, {@code "DELETED"}, {@code "STATUS_CHANGED"},
 * {@code "BALANCE_DEBITED"}, {@code "BALANCE_CREDITED"}
 *
 */
public record AuditEvent(

        UUID auditId,

        String entityId,

        String entityType,

        String action,

        String beforeState,

        String afterState,

        String actorId,

        Instant occurredAt
) {}