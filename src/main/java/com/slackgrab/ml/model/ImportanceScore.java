package com.slackgrab.ml.model;

/**
 * Result of importance scoring for a message
 *
 * Contains the discrete importance level, continuous score,
 * confidence metrics, and performance data.
 */
public record ImportanceScore(
    ImportanceLevel level,
    double score,              // Continuous score 0.0-1.0
    double confidence,         // Confidence in prediction 0.0-1.0
    double[] probabilities,    // [P(high), P(medium), P(low)]
    long inferenceTimeMs,      // Time taken for scoring
    String modelVersion        // Model version used for scoring
) {
    /**
     * Create importance score from neural network output
     *
     * @param score Raw score from network
     * @param probabilities Class probabilities
     * @param inferenceTimeMs Inference time
     * @param modelVersion Model version
     * @return ImportanceScore instance
     */
    public static ImportanceScore fromNetworkOutput(
        double score,
        double[] probabilities,
        long inferenceTimeMs,
        String modelVersion
    ) {
        ImportanceLevel level = ImportanceLevel.fromScore(score);

        // Confidence is the max probability
        double confidence = Math.max(Math.max(probabilities[0], probabilities[1]), probabilities[2]);

        return new ImportanceScore(
            level,
            score,
            confidence,
            probabilities,
            inferenceTimeMs,
            modelVersion
        );
    }

    /**
     * Create a default score (used when model not ready)
     *
     * @return Default medium importance score
     */
    public static ImportanceScore defaultScore() {
        return new ImportanceScore(
            ImportanceLevel.MEDIUM,
            0.5,
            0.0,
            new double[]{0.33, 0.34, 0.33},
            0,
            "default"
        );
    }

    /**
     * Check if this is a high confidence prediction
     *
     * @return true if confidence above threshold
     */
    public boolean isHighConfidence() {
        return confidence >= 0.75;
    }

    /**
     * Check if scoring was fast enough
     *
     * @return true if within 1 second latency target
     */
    public boolean meetsLatencyTarget() {
        return inferenceTimeMs < 1000;
    }
}
