package com.slackgrab.ml.training;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.core.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Training scheduler for background training coordination
 *
 * Manages online and batch training schedules:
 * - Online training: Continuous in background
 * - Batch training: Every 1000 interactions or daily
 *
 * Features:
 * - Automatic scheduling
 * - Training coordination
 * - Resource monitoring
 * - Graceful shutdown
 */
@Singleton
public class TrainingScheduler implements ManagedService {
    private static final Logger logger = LoggerFactory.getLogger(TrainingScheduler.class);

    private static final long BATCH_TRAINING_INTERVAL_HOURS = 24; // Daily
    private static final int BATCH_TRAINING_THRESHOLD = 1000; // Examples before batch training

    private final OnlineTrainer onlineTrainer;
    private final BatchTrainer batchTrainer;
    private final ErrorHandler errorHandler;

    private ScheduledExecutorService scheduler;
    private boolean isRunning;

    @Inject
    public TrainingScheduler(
        OnlineTrainer onlineTrainer,
        BatchTrainer batchTrainer,
        ErrorHandler errorHandler
    ) {
        this.onlineTrainer = onlineTrainer;
        this.batchTrainer = batchTrainer;
        this.errorHandler = errorHandler;
        this.isRunning = false;
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting training scheduler...");

        // Create scheduler
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setName("TrainingScheduler");
            t.setDaemon(true);
            return t;
        });

        // Start online trainer
        onlineTrainer.start();

        // Schedule periodic batch training
        scheduler.scheduleAtFixedRate(
            this::scheduledBatchTraining,
            BATCH_TRAINING_INTERVAL_HOURS,
            BATCH_TRAINING_INTERVAL_HOURS,
            TimeUnit.HOURS
        );

        // Schedule monitoring task
        scheduler.scheduleAtFixedRate(
            this::monitorTraining,
            1,
            1,
            TimeUnit.MINUTES
        );

        isRunning = true;
        logger.info("Training scheduler started");
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping training scheduler...");

        isRunning = false;

        // Stop online trainer
        onlineTrainer.stop();

        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        logger.info("Training scheduler stopped");
    }

    /**
     * Trigger batch training immediately
     *
     * Note: In production, this would query historical examples from database.
     * For now, this is a placeholder that will be called by the training trigger.
     */
    public void triggerBatchTraining() {
        if (!isRunning) {
            logger.warn("Cannot trigger batch training - scheduler not running");
            return;
        }

        scheduler.execute(this::scheduledBatchTraining);
    }

    /**
     * Scheduled batch training task
     */
    private void scheduledBatchTraining() {
        try {
            logger.info("Starting scheduled batch training");

            // Check if online trainer has processed enough examples
            int examplesTrained = onlineTrainer.getExamplesTrained();
            if (examplesTrained < BATCH_TRAINING_THRESHOLD) {
                logger.info("Not enough examples for batch training: {} < {}",
                    examplesTrained, BATCH_TRAINING_THRESHOLD);
                return;
            }

            // Check if already training
            if (batchTrainer.isTraining()) {
                logger.info("Batch training already in progress, skipping");
                return;
            }

            // In production, would query examples from database/repository
            // For now, log that batch training would occur
            logger.info("Batch training triggered (implementation pending - needs historical data)");

            // Example:
            // List<TrainingExample> examples = trainingDataRepository.getRecentExamples(1000);
            // BatchTrainer.TrainingResult result = batchTrainer.trainBatch(examples);
            // logger.info("Batch training result: {}", result);

        } catch (Exception e) {
            errorHandler.handleError("Scheduled batch training failed", e);
        }
    }

    /**
     * Monitor training status
     */
    private void monitorTraining() {
        try {
            OnlineTrainer.TrainingStats stats = onlineTrainer.getStats();
            logger.debug("Online training status: {}", stats);

            // Check if should trigger batch training based on example count
            if (stats.examplesTrained() >= BATCH_TRAINING_THRESHOLD
                && !batchTrainer.isTraining()) {
                logger.info("Example threshold reached, triggering batch training");
                triggerBatchTraining();
            }

        } catch (Exception e) {
            errorHandler.handleError("Training monitoring failed", e);
        }
    }

    /**
     * Check if scheduler is running
     *
     * @return true if running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get online trainer statistics
     *
     * @return Training stats
     */
    public OnlineTrainer.TrainingStats getOnlineStats() {
        return onlineTrainer.getStats();
    }
}
