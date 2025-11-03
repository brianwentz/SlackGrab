package com.slackgrab.ml.features;

import com.google.inject.Singleton;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Extract temporal/time-based features
 *
 * Features (5 total):
 * 1. Hour of day (normalized 0-1)
 * 2. Day of week (normalized 0-1)
 * 3. Is business hours (binary)
 * 4. Recency (how recent, normalized)
 * 5. Is weekend (binary)
 */
@Singleton
public class TemporalFeatureExtractor {

    private static final long MAX_RECENCY_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    /**
     * Extract temporal features from message timestamp
     *
     * @param messageTime Message timestamp in milliseconds
     * @param currentTime Current time in milliseconds
     * @return Array of 5 temporal features
     */
    public float[] extractFeatures(long messageTime, long currentTime) {
        float[] features = new float[5];

        ZonedDateTime messageDateTime = Instant.ofEpochMilli(messageTime)
            .atZone(ZoneId.systemDefault());

        // 0: Hour of day (normalized 0-1)
        int hour = messageDateTime.getHour();
        features[0] = (float) hour / 24.0f;

        // 1: Day of week (normalized 0-1, Monday=0, Sunday=1)
        int dayOfWeek = messageDateTime.getDayOfWeek().getValue() - 1; // 0-6
        features[1] = (float) dayOfWeek / 6.0f;

        // 2: Is business hours (9 AM - 5 PM, Monday-Friday)
        boolean isWeekday = messageDateTime.getDayOfWeek() != DayOfWeek.SATURDAY
            && messageDateTime.getDayOfWeek() != DayOfWeek.SUNDAY;
        boolean isBusinessHour = hour >= 9 && hour < 17;
        features[2] = (isWeekday && isBusinessHour) ? 1.0f : 0.0f;

        // 3: Recency (how recent the message is, 0=old, 1=very recent)
        long ageMs = currentTime - messageTime;
        if (ageMs < 0) ageMs = 0; // Handle future timestamps
        features[3] = 1.0f - Math.min(1.0f, (float) ageMs / MAX_RECENCY_MS);

        // 4: Is weekend
        features[4] = (messageDateTime.getDayOfWeek() == DayOfWeek.SATURDAY
            || messageDateTime.getDayOfWeek() == DayOfWeek.SUNDAY) ? 1.0f : 0.0f;

        return features;
    }
}
