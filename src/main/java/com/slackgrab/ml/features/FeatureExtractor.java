package com.slackgrab.ml.features;

import com.google.inject.Inject;
import com.slackgrab.data.model.SlackMessage;
import com.slackgrab.ml.model.FeatureVector;
import com.slackgrab.ml.model.ScoringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Main feature extractor for message importance scoring
 *
 * Combines text, user, media, and temporal features into a single
 * feature vector for neural network input.
 *
 * Feature dimension: 25 features total
 * - Text features: 10
 * - User features: 5
 * - Media features: 3
 * - Temporal features: 5
 * - Channel features: 2
 */
public class FeatureExtractor {
    private static final Logger logger = LoggerFactory.getLogger(FeatureExtractor.class);

    public static final int FEATURE_DIMENSION = 25;

    private final TextFeatureExtractor textExtractor;
    private final UserFeatureExtractor userExtractor;
    private final MediaFeatureExtractor mediaExtractor;
    private final TemporalFeatureExtractor temporalExtractor;

    private final Map<String, Integer> featureIndices;

    @Inject
    public FeatureExtractor(
        TextFeatureExtractor textExtractor,
        UserFeatureExtractor userExtractor,
        MediaFeatureExtractor mediaExtractor,
        TemporalFeatureExtractor temporalExtractor
    ) {
        this.textExtractor = textExtractor;
        this.userExtractor = userExtractor;
        this.mediaExtractor = mediaExtractor;
        this.temporalExtractor = temporalExtractor;
        this.featureIndices = buildFeatureIndices();
    }

    /**
     * Extract all features from a message
     *
     * @param message The message to extract features from
     * @param context Scoring context with historical data
     * @return Feature vector for neural network
     */
    public FeatureVector extractFeatures(SlackMessage message, ScoringContext context) {
        try {
            float[] features = new float[FEATURE_DIMENSION];
            int offset = 0;

            // Text features (10 features)
            float[] textFeatures = textExtractor.extractFeatures(message.text(), context);
            System.arraycopy(textFeatures, 0, features, offset, textFeatures.length);
            offset += textFeatures.length;

            // User features (5 features)
            float[] userFeatures = userExtractor.extractFeatures(message.userId(), context);
            System.arraycopy(userFeatures, 0, features, offset, userFeatures.length);
            offset += userFeatures.length;

            // Media features (3 features)
            float[] mediaFeatures = mediaExtractor.extractFeatures(message);
            System.arraycopy(mediaFeatures, 0, features, offset, mediaFeatures.length);
            offset += mediaFeatures.length;

            // Temporal features (5 features)
            float[] temporalFeatures = temporalExtractor.extractFeatures(
                parseTimestamp(message.timestamp()),
                context.getCurrentTime()
            );
            System.arraycopy(temporalFeatures, 0, features, offset, temporalFeatures.length);
            offset += temporalFeatures.length;

            // Channel features (2 features)
            float[] channelFeatures = extractChannelFeatures(message.channelId(), context);
            System.arraycopy(channelFeatures, 0, features, offset, channelFeatures.length);

            return new FeatureVector(features, featureIndices);

        } catch (Exception e) {
            logger.error("Failed to extract features from message: {}", message.id(), e);
            return createDefaultFeatureVector();
        }
    }

    /**
     * Extract channel-specific features
     */
    private float[] extractChannelFeatures(String channelId, ScoringContext context) {
        float[] features = new float[2];

        // Feature 0: Channel importance (from context)
        features[0] = (float) context.getChannelImportance(channelId);

        // Feature 1: Is private channel (heuristic based on ID)
        features[1] = channelId.startsWith("D") ? 1.0f : 0.0f; // DM channels start with 'D'

        return features;
    }

    /**
     * Parse Slack timestamp to milliseconds
     */
    private long parseTimestamp(String timestamp) {
        try {
            // Slack timestamp format: "1234567890.123456"
            double ts = Double.parseDouble(timestamp);
            return (long) (ts * 1000);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse timestamp: {}", timestamp);
            return System.currentTimeMillis();
        }
    }

    /**
     * Create default feature vector (all features = 0.5)
     */
    private FeatureVector createDefaultFeatureVector() {
        float[] features = new float[FEATURE_DIMENSION];
        for (int i = 0; i < features.length; i++) {
            features[i] = 0.5f;
        }
        return new FeatureVector(features, featureIndices);
    }

    /**
     * Build feature name to index mapping
     */
    private Map<String, Integer> buildFeatureIndices() {
        Map<String, Integer> indices = new HashMap<>();
        int idx = 0;

        // Text features (0-9)
        indices.put("text_length", idx++);
        indices.put("word_count", idx++);
        indices.put("has_question", idx++);
        indices.put("has_url", idx++);
        indices.put("has_mention", idx++);
        indices.put("has_emoji", idx++);
        indices.put("uppercase_ratio", idx++);
        indices.put("exclamation_count", idx++);
        indices.put("avg_word_length", idx++);
        indices.put("urgent_keyword_match", idx++);

        // User features (10-14)
        indices.put("sender_importance", idx++);
        indices.put("sender_frequency", idx++);
        indices.put("user_interaction_rate", idx++);
        indices.put("sender_avg_importance", idx++);
        indices.put("is_bot", idx++);

        // Media features (15-17)
        indices.put("has_attachments", idx++);
        indices.put("attachment_count", idx++);
        indices.put("in_thread", idx++);

        // Temporal features (18-22)
        indices.put("hour_of_day", idx++);
        indices.put("day_of_week", idx++);
        indices.put("is_business_hours", idx++);
        indices.put("recency", idx++);
        indices.put("is_weekend", idx++);

        // Channel features (23-24)
        indices.put("channel_importance", idx++);
        indices.put("is_private_channel", idx++);

        return indices;
    }

    /**
     * Get feature dimension
     *
     * @return Total number of features
     */
    public int getFeatureDimension() {
        return FEATURE_DIMENSION;
    }

    /**
     * Get feature indices mapping
     *
     * @return Map of feature name to index
     */
    public Map<String, Integer> getFeatureIndices() {
        return Map.copyOf(featureIndices);
    }
}
