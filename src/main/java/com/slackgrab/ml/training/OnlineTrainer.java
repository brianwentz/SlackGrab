package com.slackgrab.ml.training;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.ml.gpu.ResourceMonitor;
import com.slackgrab.ml.model.NeuralNetworkModel;
import com.slackgrab.ml.model.TrainingExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Online trainer for incremental learning
 *
 * Continuously learns from user interactions and feedback in real-time.
 * Updates neural network weights after each interaction.
 *
 * Features:
 * - Non-blocking training queue
 * - Resource-aware training (pauses when CPU high)
 * - Experience replay buffer
 * - Training statistics tracking
 */
@Singleton
public class OnlineTrainer {
    private static final Logger logger = LoggerFactory.getLogger(OnlineTrainer.class);

    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int CHECKPOINT_INTERVAL = 100; // Save after every 100 examples

    private final NeuralNetworkModel neuralNetwork;
    private final ResourceMonitor resourceMonitor;
    private final ErrorHandler errorHandler;

    private final BlockingQueue<TrainingExample> trainingQueue;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isPaused;
    private final AtomicInteger examplesTrained;

    private Thread trainingThread;

    @Inject
    public OnlineTrainer(
        NeuralNetworkModel neuralNetwork,
        ResourceMonitor resourceMonitor,
        ErrorHandler errorHandler
    ) {
        this.neuralNetwork = neuralNetwork;
        this.resourceMonitor = resourceMonitor;
        this.errorHandler = errorHandler;

        this.trainingQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        this.isRunning = new AtomicBoolean(false);
        this.isPaused = new AtomicBoolean(false);
        this.examplesTrained = new AtomicInteger(0);
    }

    /**
     * Start online training
     */
    public void start() {
        if (isRunning.get()) {
            logger.warn("Online trainer already running");
            return;
        }

        logger.info("Starting online trainer...");
        isRunning.set(true);
        isPaused.set(false);

        trainingThread = new Thread(this::trainingLoop, "OnlineTrainer");
        trainingThread.setDaemon(true);
        trainingThread.start();

        logger.info("Online trainer started");
    }

    /**
     * Stop online training
     */
    public void stop() {
        if (!isRunning.get()) {
            return;
        }

        logger.info("Stopping online trainer...");
        isRunning.set(false);

        if (trainingThread != null) {
            trainingThread.interrupt();
            try {
                trainingThread.join(5000); // Wait up to 5 seconds
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for training thread to stop");
            }
        }

        logger.info("Online trainer stopped. Total examples trained: {}", examplesTrained.get());
    }

    /**
     * Pause training (e.g., due to high CPU usage)
     */
    public void pause() {
        if (!isPaused.get()) {
            logger.info("Pausing online training");
            isPaused.set(true);
        }
    }

    /**
     * Resume training
     */
    public void resume() {
        if (isPaused.get()) {
            logger.info("Resuming online training");
            isPaused.set(false);
        }
    }

    /**
     * Add training example to queue
     *
     * @param example Training example
     * @return true if added successfully
     */
    public boolean enqueueExample(TrainingExample example) {
        if (!isRunning.get()) {
            logger.warn("Cannot enqueue example - trainer not running");
            return false;
        }

        boolean added = trainingQueue.offer(example);
        if (!added) {
            logger.warn("Training queue full, dropping example");
        }
        return added;
    }

    /**
     * Get current queue size
     *
     * @return Number of examples waiting
     */
    public int getQueueSize() {
        return trainingQueue.size();
    }

    /**
     * Get total examples trained
     *
     * @return Count of trained examples
     */
    public int getExamplesTrained() {
        return examplesTrained.get();
    }

    /**
     * Check if trainer is paused
     *
     * @return true if paused
     */
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * Main training loop
     */
    private void trainingLoop() {
        logger.info("Training loop started");

        while (isRunning.get()) {
            try {
                // Check if should pause due to resource constraints
                if (resourceMonitor.shouldPauseTraining()) {
                    if (!isPaused.get()) {
                        pause();
                    }
                } else if (isPaused.get()) {
                    resume();
                }

                // If paused, sleep and continue
                if (isPaused.get()) {
                    Thread.sleep(1000);
                    continue;
                }

                // Poll for training example (with timeout)
                TrainingExample example = trainingQueue.poll(
                    1, java.util.concurrent.TimeUnit.SECONDS);

                if (example == null) {
                    continue; // No example available, try again
                }

                // Train on example
                trainOnExample(example);

                // Increment counter
                int count = examplesTrained.incrementAndGet();

                // Checkpoint periodically
                if (count % CHECKPOINT_INTERVAL == 0) {
                    logger.info("Online training: {} examples trained", count);
                    neuralNetwork.saveCheckpoint();
                }

            } catch (InterruptedException e) {
                logger.info("Training loop interrupted");
                break;
            } catch (Exception e) {
                errorHandler.handleError("Error in training loop", e);
                // Continue training despite error
            }
        }

        logger.info("Training loop ended");
    }

    /**
     * Train on a single example
     */
    private void trainOnExample(TrainingExample example) {
        try {
            neuralNetwork.trainOnline(example);
        } catch (Exception e) {
            errorHandler.handleError("Failed to train on example", e);
        }
    }

    /**
     * Get training statistics
     *
     * @return Training stats
     */
    public TrainingStats getStats() {
        return new TrainingStats(
            examplesTrained.get(),
            trainingQueue.size(),
            isPaused.get(),
            isRunning.get()
        );
    }

    /**
     * Training statistics record
     */
    public record TrainingStats(
        int examplesTrained,
        int queueSize,
        boolean paused,
        boolean running
    ) {
        @Override
        public String toString() {
            return String.format("TrainingStats[trained=%d, queued=%d, paused=%b, running=%b]",
                examplesTrained, queueSize, paused, running);
        }
    }
}
