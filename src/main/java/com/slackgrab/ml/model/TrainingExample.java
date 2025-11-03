package com.slackgrab.ml.model;

/**
 * Training example for neural network learning
 *
 * Pairs a feature vector with its target importance score,
 * used for both online and batch training.
 */
public record TrainingExample(
    FeatureVector features,
    double targetScore,           // Ground truth score 0.0-1.0
    ImportanceLevel targetLevel,   // Ground truth level
    long timestamp                // When this example was created
) {
    /**
     * Create training example from user feedback
     *
     * @param features Feature vector
     * @param feedback Feedback type
     * @param originalScore Original score from model
     * @return Training example
     */
    public static TrainingExample fromFeedback(
        FeatureVector features,
        FeedbackType feedback,
        double originalScore
    ) {
        double targetScore = adjustScoreFromFeedback(originalScore, feedback);
        ImportanceLevel targetLevel = ImportanceLevel.fromScore(targetScore);

        return new TrainingExample(
            features,
            targetScore,
            targetLevel,
            System.currentTimeMillis()
        );
    }

    /**
     * Create training example from user interaction
     *
     * @param features Feature vector
     * @param interacted Whether user interacted with message
     * @param dwellTime Time spent on message (ms)
     * @return Training example
     */
    public static TrainingExample fromInteraction(
        FeatureVector features,
        boolean interacted,
        long dwellTime
    ) {
        // Score based on interaction intensity
        double targetScore;
        if (!interacted) {
            targetScore = 0.2; // Low importance if not interacted
        } else if (dwellTime > 10000) { // >10 seconds
            targetScore = 0.9; // High importance for long dwell
        } else if (dwellTime > 2000) { // >2 seconds
            targetScore = 0.6; // Medium importance
        } else {
            targetScore = 0.4; // Lower medium
        }

        return new TrainingExample(
            features,
            targetScore,
            ImportanceLevel.fromScore(targetScore),
            System.currentTimeMillis()
        );
    }

    /**
     * Adjust score based on feedback type
     */
    private static double adjustScoreFromFeedback(double original, FeedbackType feedback) {
        return switch (feedback) {
            case TOO_LOW -> Math.min(1.0, original + 0.3);
            case TOO_HIGH -> Math.max(0.0, original - 0.3);
            case GOOD -> original;
        };
    }

    /**
     * Check if example is recent enough for training
     *
     * @param maxAgeMs Maximum age in milliseconds
     * @return true if example is fresh
     */
    public boolean isFresh(long maxAgeMs) {
        return (System.currentTimeMillis() - timestamp) < maxAgeMs;
    }
}
