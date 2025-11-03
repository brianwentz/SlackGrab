package com.slackgrab.ml.features;

import com.google.inject.Singleton;
import com.slackgrab.data.model.SlackMessage;

/**
 * Extract media/attachment-based features
 *
 * Features (3 total):
 * 1. Has attachments (binary)
 * 2. Attachment count (normalized)
 * 3. Is in thread (binary)
 */
@Singleton
public class MediaFeatureExtractor {

    /**
     * Extract media features from message
     *
     * @param message The message to extract from
     * @return Array of 3 media features
     */
    public float[] extractFeatures(SlackMessage message) {
        float[] features = new float[3];

        // 0: Has attachments
        features[0] = message.hasAttachments() ? 1.0f : 0.0f;

        // 1: Attachment count (estimated)
        // In production, would get actual count from message data
        features[1] = message.hasAttachments() ? 0.5f : 0.0f;

        // 2: Is in thread
        features[2] = message.isInThread() ? 1.0f : 0.0f;

        return features;
    }
}
