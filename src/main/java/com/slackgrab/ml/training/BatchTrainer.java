package com.slackgrab.ml.training;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.ml.gpu.ResourceMonitor;
import com.slackgrab.ml.model.NeuralNetworkModel;
import com.slackgrab.ml.model.TrainingExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch trainer for periodic model retraining
 *
 * Performs full retraining passes over historical data to improve
 * model stability and performance.
 *
 * Features:
 * - Batch training with multiple epochs
 * - Resource-aware execution
 * - Training metrics tracking
 * - Automatic checkpointing
 */
@Singleton
public class BatchTrainer {
    private static final Logger logger = LoggerFactory.getLogger(BatchTrainer.class);

    private static final int DEFAULT_EPOCHS = 5;
    private static final int MIN_BATCH_SIZE = 32;

    private final NeuralNetworkModel neuralNetwork;
    private final ResourceMonitor resourceMonitor;
    private final ErrorHandler errorHandler;

    private volatile boolean isTraining;

    @Inject
    public BatchTrainer(
        NeuralNetworkModel neuralNetwork,
        ResourceMonitor resourceMonitor,
        ErrorHandler errorHandler
    ) {
        this.neuralNetwork = neuralNetwork;
        this.resourceMonitor = resourceMonitor;
        this.errorHandler = errorHandler;
        this.isTraining = false;
    }

    /**
     * Train on a batch of examples
     *
     * @param examples Training examples
     * @return Training result
     */
    public TrainingResult trainBatch(List<TrainingExample> examples) {
        return trainBatch(examples, DEFAULT_EPOCHS);
    }

    /**
     * Train on a batch of examples with specified epochs
     *
     * @param examples Training examples
     * @param epochs Number of training epochs
     * @return Training result
     */
    public TrainingResult trainBatch(List<TrainingExample> examples, int epochs) {
        if (isTraining) {
            logger.warn("Batch training already in progress");
            return TrainingResult.failed("Training already in progress");
        }

        if (examples.isEmpty()) {
            logger.warn("No examples provided for batch training");
            return TrainingResult.failed("No examples provided");
        }

        if (examples.size() < MIN_BATCH_SIZE) {
            logger.warn("Batch size too small: {} < {}", examples.size(), MIN_BATCH_SIZE);
            return TrainingResult.failed("Batch size too small");
        }

        isTraining = true;
        long startTime = System.currentTimeMillis();

        try {
            logger.info("Starting batch training: {} examples, {} epochs",
                examples.size(), epochs);

            // Check resources before starting
            if (!resourceMonitor.isWithinLimits()) {
                logger.warn("Resources exceed limits, deferring batch training");
                return TrainingResult.deferred("Resource limits exceeded");
            }

            // Convert to array for neural network
            TrainingExample[] exampleArray = examples.toArray(new TrainingExample[0]);

            // Train
            neuralNetwork.trainBatch(exampleArray, epochs);

            // Save checkpoint after training
            String checkpointPath = neuralNetwork.saveCheckpoint();

            long duration = System.currentTimeMillis() - startTime;

            logger.info("Batch training completed in {} ms", duration);

            return TrainingResult.success(
                examples.size(),
                duration,
                checkpointPath
            );

        } catch (Exception e) {
            errorHandler.handleError("Batch training failed", e);
            long duration = System.currentTimeMillis() - startTime;
            return TrainingResult.failed("Training failed: " + e.getMessage());

        } finally {
            isTraining = false;
        }
    }

    /**
     * Check if batch training is in progress
     *
     * @return true if training
     */
    public boolean isTraining() {
        return isTraining;
    }

    /**
     * Training result record
     */
    public record TrainingResult(
        Status status,
        int exampleCount,
        long durationMs,
        String checkpointPath,
        String message
    ) {
        public enum Status {
            SUCCESS,
            FAILED,
            DEFERRED
        }

        public static TrainingResult success(int count, long duration, String checkpoint) {
            return new TrainingResult(
                Status.SUCCESS,
                count,
                duration,
                checkpoint,
                "Training completed successfully"
            );
        }

        public static TrainingResult failed(String message) {
            return new TrainingResult(
                Status.FAILED,
                0,
                0,
                null,
                message
            );
        }

        public static TrainingResult deferred(String message) {
            return new TrainingResult(
                Status.DEFERRED,
                0,
                0,
                null,
                message
            );
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        @Override
        public String toString() {
            return String.format("TrainingResult[status=%s, examples=%d, duration=%dms, %s]",
                status, exampleCount, durationMs, message);
        }
    }
}
