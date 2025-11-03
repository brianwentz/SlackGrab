package com.slackgrab.data.model;

import java.time.Instant;

/**
 * Slack channel data model
 *
 * Represents a Slack channel stored in local database.
 * Maps to the 'channels' table in SQLite.
 */
public record SlackChannel(
    String id,                // Channel ID
    String name,              // Channel name (e.g., "general")
    boolean isPrivate,        // True if private channel
    int memberCount,          // Number of members in channel
    Instant lastSynced        // Last time we synced messages from this channel
) {
    /**
     * Create a new channel without sync time
     */
    public static SlackChannel createNew(
        String id,
        String name,
        boolean isPrivate,
        int memberCount
    ) {
        return new SlackChannel(
            id,
            name,
            isPrivate,
            memberCount,
            null
        );
    }

    /**
     * Create a copy with updated sync time
     */
    public SlackChannel withSyncTime(Instant syncTime) {
        return new SlackChannel(
            id,
            name,
            isPrivate,
            memberCount,
            syncTime
        );
    }

    /**
     * Check if channel has been synced
     */
    public boolean hasBeenSynced() {
        return lastSynced != null;
    }
}
