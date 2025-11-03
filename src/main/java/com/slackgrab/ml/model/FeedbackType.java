package com.slackgrab.ml.model;

/**
 * Three-level user feedback on message importance predictions
 *
 * Provides simple feedback mechanism without requiring explanation.
 */
public enum FeedbackType {
    TOO_LOW,   // Model scored too low (should be higher importance)
    GOOD,      // Model score was appropriate
    TOO_HIGH;  // Model scored too high (should be lower importance)

    /**
     * Parse feedback from string
     *
     * @param value String value
     * @return FeedbackType
     */
    public static FeedbackType fromString(String value) {
        return valueOf(value.toUpperCase().replace(' ', '_'));
    }
}
