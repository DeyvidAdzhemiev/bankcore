package com.bankcore.common.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by {@code fraud-service} after evaluating a transaction risk score.
 *
 * <p><strong>Topic:</strong> {@code fraud.decision}
 * <br><strong>Consumers:</strong> {@code transaction-service} (acts on the decision),
 * {@code audit-service}
 *
 * <h2>Decision values</h2>
 * <ul>
 *   <li>{@code "APPROVE"} — transaction should proceed to settlement.</li>
 *   <li>{@code "REVIEW"}  — transaction is flagged for manual review; hold settlement.</li>
 *   <li>{@code "BLOCK"}   — transaction should be rejected immediately.</li>
 * </ul>
 */
public record FraudDecisionEvent(

        UUID fraudScoreId,

        UUID transactionId,

        double score,

        String decision,

        List<String> triggeredRules,

        Instant evaluatedAt
) {}