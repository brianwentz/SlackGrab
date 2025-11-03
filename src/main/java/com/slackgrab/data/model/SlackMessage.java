package com.slackgrab.data.model;

import java.time.Instant;

/**
 * Slack message data model
 *
 * Represents a Slack message stored in local database.
 * Maps to the 'messages' table in SQLite.
 */
public record SlackMessage(
    String id,                    // Message timestamp (serves as unique ID)
    String channelId,             // Channel where message was posted
    String userId,                // User who posted the message
    String text,                  // Message text content
    String timestamp,             // Message timestamp (Slack format)
    String threadTs,              // Thread timestamp (null if not in thread)
    boolean hasAttachments,       // True if message has files/attachments
    boolean hasReactions,         // True if message has emoji reactions
    Double importanceScore,       // Neural network importance score (0.0-1.0)
    String importanceLevel,       // Importance level (HIGH/MEDIUM/LOW)
    Instant createdAt             // When we stored this message locally
) {
    /**
     * Create a new message without importance scoring
     */
    public static SlackMessage createNew(
        String id,
        String channelId,
        String userId,
        String text,
        String timestamp,
        String threadTs,
        boolean hasAttachments,
        boolean hasReactions
    ) {
        return new SlackMessage(
            id,
            channelId,
            userId,
            text,
            timestamp,
            threadTs,
            hasAttachments,
            hasReactions,
            null,  // Score will be calculated later
            null,  // Level will be calculated later
            Instant.now()
        );
    }

    /**
     * Create a copy with updated importance scoring
     */
    public SlackMessage withImportance(Double score, String level) {
        return new SlackMessage(
            id,
            channelId,
            userId,
            text,
            timestamp,
            threadTs,
            hasAttachments,
            hasReactions,
            score,
            level,
            createdAt
        );
    }

    /**
     * Check if message is part of a thread
     */
    public boolean isInThread() {
        return threadTs != null && !threadTs.isEmpty();
    }

    /**
     * Check if message has been scored
     */
    public boolean hasImportanceScore() {
        return importanceScore != null;
    }
}
