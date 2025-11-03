package com.slackgrab.ml.model;

/**
 * Three-level importance classification for messages
 *
 * Matches user-facing importance levels displayed in Slack Apps UI.
 */
public enum ImportanceLevel {
    HIGH,
    MEDIUM,
    LOW;

    /**
     * Convert a continuous score (0.0-1.0) to discrete importance level
     *
     * @param score Importance score from neural network
     * @return Corresponding importance level
     */
    public static ImportanceLevel fromScore(double score) {
        if (score >= 0.67) {
            return HIGH;
        } else if (score >= 0.33) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }

    /**
     * Get the midpoint score for this level
     *
     * @return Representative score for this level
     */
    public double getMidpointScore() {
        return switch (this) {
            case HIGH -> 0.85;
            case MEDIUM -> 0.50;
            case LOW -> 0.15;
        };
    }
}
