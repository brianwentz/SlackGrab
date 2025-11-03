package com.slackgrab.ml.features;

import com.google.inject.Singleton;
import com.slackgrab.ml.model.ScoringContext;

/**
 * Extract user/sender-based features
 *
 * Features (5 total):
 * 1. Sender importance (from context/history)
 * 2. Sender frequency (message volume)
 * 3. User interaction rate with sender
 * 4. Sender's average importance score
 * 5. Is bot (binary)
 */
@Singleton
public class UserFeatureExtractor {

    /**
     * Extract user features for a sender
     *
     * @param senderId Sender user ID
     * @param context Scoring context with historical data
     * @return Array of 5 user features
     */
    public float[] extractFeatures(String senderId, ScoringContext context) {
        float[] features = new float[5];

        // 0: Sender importance (from context)
        features[0] = (float) context.getSenderImportance(senderId);

        // 1: Sender frequency (estimated from ID hash for now)
        // In production, this would come from actual message counts
        features[1] = estimateSenderFrequency(senderId);

        // 2: User interaction rate with sender
        // In production, this would be calculated from interaction history
        features[2] = 0.5f; // Default neutral value

        // 3: Sender's average importance score
        // Would be calculated from historical scores
        features[3] = (float) context.getSenderImportance(senderId);

        // 4: Is bot (heuristic based on ID pattern)
        features[4] = isLikelyBot(senderId) ? 1.0f : 0.0f;

        return features;
    }

    /**
     * Estimate sender frequency (placeholder)
     *
     * In production, this would query actual message counts.
     * For now, use a simple hash-based estimate.
     */
    private float estimateSenderFrequency(String senderId) {
        // Normalize to 0.0-1.0 range
        int hash = Math.abs(senderId.hashCode());
        return (float) (hash % 100) / 100.0f;
    }

    /**
     * Check if sender is likely a bot
     *
     * Bots typically have IDs starting with 'B'
     */
    private boolean isLikelyBot(String senderId) {
        return senderId != null && senderId.startsWith("B");
    }
}
