package com.slackgrab.ml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.data.model.SlackMessage;
import com.slackgrab.ml.features.FeatureExtractor;
import com.slackgrab.ml.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main interface for message importance scoring
 *
 * Coordinates feature extraction and neural network inference
 * to produce importance scores for Slack messages.
 */
@Singleton
public class ImportanceScorer {
    private static final Logger logger = LoggerFactory.getLogger(ImportanceScorer.class);

    private final FeatureExtractor featureExtractor;
    private final NeuralNetworkModel neuralNetwork;
    private final ErrorHandler errorHandler;

    @Inject
    public ImportanceScorer(
        FeatureExtractor featureExtractor,
        NeuralNetworkModel neuralNetwork,
        ErrorHandler errorHandler
    ) {
        this.featureExtractor = featureExtractor;
        this.neuralNetwork = neuralNetwork;
        this.errorHandler = errorHandler;
    }

    /**
     * Initialize the scorer
     *
     * @return true if initialization successful
     */
    public boolean initialize() {
        logger.info("Initializing ImportanceScorer...");

        // Initialize neural network
        if (!neuralNetwork.initialize()) {
            logger.error("Failed to initialize neural network");
            return false;
        }

        logger.info("ImportanceScorer initialized successfully");
        return true;
    }

    /**
     * Score a single message for importance
     *
     * @param message Message to score
     * @return Importance score
     */
    public ImportanceScore score(SlackMessage message) {
        return score(message, ScoringContext.createDefault());
    }

    /**
     * Score a single message with context
     *
     * @param message Message to score
     * @param context Scoring context with historical data
     * @return Importance score
     */
    public ImportanceScore score(SlackMessage message, ScoringContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // If model not ready, return default score
            if (!neuralNetwork.isReady()) {
                logger.warn("Neural network not ready, returning default score");
                return ImportanceScore.defaultScore();
            }

            // Extract features
            FeatureVector features = featureExtractor.extractFeatures(message, context);

            // Score with neural network
            double score = neuralNetwork.score(features);

            // Calculate probabilities (simplified - in production would use softmax)
            double[] probabilities = calculateProbabilities(score);

            // Calculate inference time
            long inferenceTime = System.currentTimeMillis() - startTime;

            // Create importance score
            ImportanceScore importanceScore = ImportanceScore.fromNetworkOutput(
                score,
                probabilities,
                inferenceTime,
                neuralNetwork.getModelVersion()
            );

            // Log slow inferences
            if (!importanceScore.meetsLatencyTarget()) {
                logger.warn("Slow inference: {} ms for message {}", inferenceTime, message.id());
            }

            return importanceScore;

        } catch (Exception e) {
            errorHandler.handleError("Failed to score message: " + message.id(), e);
            return ImportanceScore.defaultScore();
        }
    }

    /**
     * Batch score multiple messages
     *
     * @param messages Messages to score
     * @return Array of importance scores
     */
    public ImportanceScore[] batchScore(SlackMessage[] messages) {
        return batchScore(messages, ScoringContext.createDefault());
    }

    /**
     * Batch score multiple messages with context
     *
     * @param messages Messages to score
     * @param context Scoring context
     * @return Array of importance scores
     */
    public ImportanceScore[] batchScore(SlackMessage[] messages, ScoringContext context) {
        long startTime = System.currentTimeMillis();

        try {
            if (!neuralNetwork.isReady()) {
                logger.warn("Neural network not ready, returning default scores");
                ImportanceScore[] scores = new ImportanceScore[messages.length];
                for (int i = 0; i < scores.length; i++) {
                    scores[i] = ImportanceScore.defaultScore();
                }
                return scores;
            }

            // Extract features for all messages
            FeatureVector[] features = new FeatureVector[messages.length];
            for (int i = 0; i < messages.length; i++) {
                features[i] = featureExtractor.extractFeatures(messages[i], context);
            }

            // Batch score
            double[] rawScores = neuralNetwork.batchScore(features);

            // Create importance scores
            long inferenceTime = System.currentTimeMillis() - startTime;
            ImportanceScore[] importanceScores = new ImportanceScore[messages.length];

            for (int i = 0; i < messages.length; i++) {
                double[] probabilities = calculateProbabilities(rawScores[i]);
                importanceScores[i] = ImportanceScore.fromNetworkOutput(
                    rawScores[i],
                    probabilities,
                    inferenceTime / messages.length, // Amortized time
                    neuralNetwork.getModelVersion()
                );
            }

            logger.info("Batch scored {} messages in {} ms", messages.length, inferenceTime);
            return importanceScores;

        } catch (Exception e) {
            errorHandler.handleError("Failed to batch score messages", e);
            ImportanceScore[] scores = new ImportanceScore[messages.length];
            for (int i = 0; i < scores.length; i++) {
                scores[i] = ImportanceScore.defaultScore();
            }
            return scores;
        }
    }

    /**
     * Calculate class probabilities from continuous score
     *
     * Simplified approach - in production would use softmax output layer
     *
     * @param score Continuous score 0.0-1.0
     * @return [P(high), P(medium), P(low)]
     */
    private double[] calculateProbabilities(double score) {
        double[] probs = new double[3];

        if (score >= 0.67) {
            // High importance
            probs[0] = score;
            probs[1] = 1.0 - score;
            probs[2] = 0.0;
        } else if (score >= 0.33) {
            // Medium importance
            probs[0] = (score - 0.33) / 0.34;
            probs[1] = 1.0 - Math.abs(score - 0.5) * 2;
            probs[2] = (0.67 - score) / 0.34;
        } else {
            // Low importance
            probs[0] = 0.0;
            probs[1] = score;
            probs[2] = 1.0 - score;
        }

        // Normalize
        double sum = probs[0] + probs[1] + probs[2];
        if (sum > 0) {
            probs[0] /= sum;
            probs[1] /= sum;
            probs[2] /= sum;
        }

        return probs;
    }

    /**
     * Check if scorer is ready
     *
     * @return true if ready to score messages
     */
    public boolean isReady() {
        return neuralNetwork.isReady();
    }

    /**
     * Get current model version
     *
     * @return Model version string
     */
    public String getModelVersion() {
        return neuralNetwork.getModelVersion();
    }

    /**
     * Shutdown the scorer
     */
    public void shutdown() {
        logger.info("Shutting down ImportanceScorer...");
        neuralNetwork.shutdown();
    }
}
