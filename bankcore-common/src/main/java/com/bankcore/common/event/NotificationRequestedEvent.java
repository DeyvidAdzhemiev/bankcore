package com.bankcore.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Requests delivery of a notification to a user via a specific channel.
 * Published by any service that needs to send a user-facing message.
 *
 * <p><strong>Topic:</strong> {@code notification.requested}
 * <br><strong>Consumer:</strong> {@code notification-service}
 *
 * <h2>Template keys (examples)</h2>
 * <ul>
 *   <li>{@code "transaction.completed"} — "Your transfer of $50.00 is complete."</li>
 *   <li>{@code "transaction.failed"}    — "Your transfer could not be processed."</li>
 *   <li>{@code "fraud.blocked"}         — "A suspicious transaction was blocked."</li>
 *   <li>{@code "account.frozen"}        — "Your account has been temporarily frozen."</li>
 * </ul>
 */
public record NotificationRequestedEvent(

        UUID notificationId,

        UUID userId,

        String channel,

        String templateKey,

        Map<String, String> payload,

        Instant requestedAt
) {}