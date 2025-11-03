package com.slackgrab.ml.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context information for message importance scoring
 *
 * Provides historical patterns, sender/channel importance,
 * and user behavior data to enhance scoring accuracy.
 */
public class ScoringContext {
    private final Map<String, Double> senderImportance;
    private final Map<String, Double> channelImportance;
    private final List<String> urgentKeywords;
    private final long currentTime;

    private ScoringContext(Builder builder) {
        this.senderImportance = Map.copyOf(builder.senderImportance);
        this.channelImportance = Map.copyOf(builder.channelImportance);
        this.urgentKeywords = List.copyOf(builder.urgentKeywords);
        this.currentTime = builder.currentTime;
    }

    /**
     * Get sender importance score
     *
     * @param senderId Sender ID
     * @return Importance score (0.0-1.0), default 0.5
     */
    public double getSenderImportance(String senderId) {
        return senderImportance.getOrDefault(senderId, 0.5);
    }

    /**
     * Get channel importance score
     *
     * @param channelId Channel ID
     * @return Importance score (0.0-1.0), default 0.5
     */
    public double getChannelImportance(String channelId) {
        return channelImportance.getOrDefault(channelId, 0.5);
    }

    /**
     * Get urgent keywords list
     *
     * @return List of keywords indicating urgency
     */
    public List<String> getUrgentKeywords() {
        return urgentKeywords;
    }

    /**
     * Get current time for temporal features
     *
     * @return Current time in milliseconds
     */
    public long getCurrentTime() {
        return currentTime;
    }

    /**
     * Create a default scoring context
     *
     * @return Default context with no historical data
     */
    public static ScoringContext createDefault() {
        return new Builder()
            .withCurrentTime(System.currentTimeMillis())
            .build();
    }

    /**
     * Builder for ScoringContext
     */
    public static class Builder {
        private Map<String, Double> senderImportance = new HashMap<>();
        private Map<String, Double> channelImportance = new HashMap<>();
        private List<String> urgentKeywords = List.of();
        private long currentTime = System.currentTimeMillis();

        public Builder withSenderImportance(Map<String, Double> senderImportance) {
            this.senderImportance = new HashMap<>(senderImportance);
            return this;
        }

        public Builder addSenderImportance(String senderId, double importance) {
            this.senderImportance.put(senderId, importance);
            return this;
        }

        public Builder withChannelImportance(Map<String, Double> channelImportance) {
            this.channelImportance = new HashMap<>(channelImportance);
            return this;
        }

        public Builder addChannelImportance(String channelId, double importance) {
            this.channelImportance.put(channelId, importance);
            return this;
        }

        public Builder withUrgentKeywords(List<String> keywords) {
            this.urgentKeywords = List.copyOf(keywords);
            return this;
        }

        public Builder withCurrentTime(long currentTime) {
            this.currentTime = currentTime;
            return this;
        }

        public ScoringContext build() {
            return new ScoringContext(this);
        }
    }
}
