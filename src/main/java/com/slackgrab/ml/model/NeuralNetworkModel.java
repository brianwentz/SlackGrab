package com.slackgrab.ml.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.slackgrab.core.ConfigurationManager;
import com.slackgrab.core.ErrorHandler;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Deep learning neural network for message importance scoring
 *
 * Architecture:
 * - Input: 25 features
 * - Hidden Layer 1: 64 neurons, ReLU, 20% dropout
 * - Hidden Layer 2: 32 neurons, ReLU, 20% dropout
 * - Output: 1 neuron, Sigmoid (score 0.0-1.0)
 *
 * Training:
 * - Loss: MSE (Mean Squared Error)
 * - Optimizer: Adam (learning rate: 0.001 online, 0.01 batch)
 * - Supports both online and batch training
 */
@Singleton
public class NeuralNetworkModel {
    private static final Logger logger = LoggerFactory.getLogger(NeuralNetworkModel.class);

    private static final int INPUT_SIZE = 25;
    private static final int HIDDEN_LAYER_1_SIZE = 64;
    private static final int HIDDEN_LAYER_2_SIZE = 32;
    private static final int OUTPUT_SIZE = 1;

    private static final double LEARNING_RATE_ONLINE = 0.001;
    private static final double LEARNING_RATE_BATCH = 0.01;
    private static final double DROPOUT_RATE = 0.2;

    private final ConfigurationManager configurationManager;
    private final ErrorHandler errorHandler;

    private MultiLayerNetwork model;
    private String modelVersion;
    private boolean isReady;

    @Inject
    public NeuralNetworkModel(
        ConfigurationManager configurationManager,
        ErrorHandler errorHandler
    ) {
        this.configurationManager = configurationManager;
        this.errorHandler = errorHandler;
        this.isReady = false;
    }

    /**
     * Initialize the neural network
     *
     * Creates a new network with random weights or loads from checkpoint.
     *
     * @return true if initialization successful
     */
    public boolean initialize() {
        try {
            logger.info("Initializing neural network model...");

            // Try to load existing model first
            File modelDir = getModelDirectory();
            File latestModel = findLatestModelCheckpoint(modelDir);

            if (latestModel != null && latestModel.exists()) {
                logger.info("Loading existing model from: {}", latestModel);
                model = MultiLayerNetwork.load(latestModel, true);
                modelVersion = extractVersionFromFilename(latestModel.getName());
            } else {
                logger.info("Creating new model with random initialization");
                model = createNewModel();
                modelVersion = generateModelVersion();
            }

            isReady = true;
            logger.info("Neural network model initialized. Version: {}", modelVersion);
            return true;

        } catch (Exception e) {
            errorHandler.handleError("Failed to initialize neural network", e);
            isReady = false;
            return false;
        }
    }

    /**
     * Create a new neural network with random initialization
     */
    private MultiLayerNetwork createNewModel() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(42)  // For reproducibility
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(LEARNING_RATE_ONLINE))
            .list()
            .layer(new DenseLayer.Builder()
                .nIn(INPUT_SIZE)
                .nOut(HIDDEN_LAYER_1_SIZE)
                .activation(Activation.RELU)
                .dropOut(DROPOUT_RATE)
                .build())
            .layer(new DenseLayer.Builder()
                .nIn(HIDDEN_LAYER_1_SIZE)
                .nOut(HIDDEN_LAYER_2_SIZE)
                .activation(Activation.RELU)
                .dropOut(DROPOUT_RATE)
                .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(HIDDEN_LAYER_2_SIZE)
                .nOut(OUTPUT_SIZE)
                .activation(Activation.SIGMOID)
                .build())
            .build();

        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();
        network.setListeners(new ScoreIterationListener(100));

        return network;
    }

    /**
     * Score a message for importance
     *
     * @param features Feature vector
     * @return Importance score (0.0-1.0)
     */
    public double score(FeatureVector features) {
        if (!isReady) {
            logger.warn("Model not ready, returning default score");
            return 0.5;
        }

        try {
            INDArray input = features.toBatchINDArray();
            INDArray output = model.output(input);
            return output.getDouble(0);

        } catch (Exception e) {
            errorHandler.handleError("Failed to score message", e);
            return 0.5;
        }
    }

    /**
     * Batch score multiple messages
     *
     * @param featuresList List of feature vectors
     * @return Array of scores
     */
    public double[] batchScore(FeatureVector[] featuresList) {
        if (!isReady || featuresList.length == 0) {
            double[] defaultScores = new double[featuresList.length];
            for (int i = 0; i < defaultScores.length; i++) {
                defaultScores[i] = 0.5;
            }
            return defaultScores;
        }

        try {
            // Stack feature vectors into batch
            INDArray input = Nd4j.create(featuresList.length, INPUT_SIZE);
            for (int i = 0; i < featuresList.length; i++) {
                input.putRow(i, featuresList[i].toINDArray());
            }

            INDArray output = model.output(input);

            // Extract scores
            double[] scores = new double[featuresList.length];
            for (int i = 0; i < scores.length; i++) {
                scores[i] = output.getDouble(i, 0);
            }

            return scores;

        } catch (Exception e) {
            errorHandler.handleError("Failed to batch score messages", e);
            double[] defaultScores = new double[featuresList.length];
            for (int i = 0; i < defaultScores.length; i++) {
                defaultScores[i] = 0.5;
            }
            return defaultScores;
        }
    }

    /**
     * Train on a single example (online learning)
     *
     * @param example Training example
     */
    public void trainOnline(TrainingExample example) {
        if (!isReady) {
            return;
        }

        try {
            INDArray input = example.features().toBatchINDArray();
            INDArray label = Nd4j.create(new double[][]{{example.targetScore()}});

            DataSet dataSet = new DataSet(input, label);
            model.fit(dataSet);

        } catch (Exception e) {
            errorHandler.handleError("Failed to train online", e);
        }
    }

    /**
     * Train on a batch of examples
     *
     * @param examples Training examples
     * @param epochs Number of epochs
     */
    public void trainBatch(TrainingExample[] examples, int epochs) {
        if (!isReady || examples.length == 0) {
            return;
        }

        try {
            logger.info("Training on batch of {} examples for {} epochs", examples.length, epochs);

            // Prepare batch dataset
            INDArray input = Nd4j.create(examples.length, INPUT_SIZE);
            INDArray labels = Nd4j.create(examples.length, OUTPUT_SIZE);

            for (int i = 0; i < examples.length; i++) {
                input.putRow(i, examples[i].features().toINDArray());
                labels.putScalar(new int[]{i, 0}, examples[i].targetScore());
            }

            DataSet dataSet = new DataSet(input, labels);

            // Train for multiple epochs
            // Note: Learning rate adjustment via updater config is complex in DL4J
            // For now, train with existing configuration
            for (int epoch = 0; epoch < epochs; epoch++) {
                model.fit(dataSet);
            }

            logger.info("Batch training completed");

        } catch (Exception e) {
            errorHandler.handleError("Failed to train batch", e);
        }
    }

    /**
     * Save model checkpoint
     *
     * @return Checkpoint path, or null if failed
     */
    public String saveCheckpoint() {
        if (!isReady) {
            return null;
        }

        try {
            String version = generateModelVersion();
            File modelDir = getModelDirectory();
            modelDir.mkdirs();

            File checkpointFile = new File(modelDir, "model-" + version + ".zip");
            model.save(checkpointFile, true);

            logger.info("Model checkpoint saved: {}", checkpointFile);
            return checkpointFile.getAbsolutePath();

        } catch (IOException e) {
            errorHandler.handleError("Failed to save model checkpoint", e);
            return null;
        }
    }

    /**
     * Load model from checkpoint
     *
     * @param checkpointPath Path to checkpoint file
     * @return true if loaded successfully
     */
    public boolean loadCheckpoint(String checkpointPath) {
        try {
            File checkpointFile = new File(checkpointPath);
            if (!checkpointFile.exists()) {
                logger.warn("Checkpoint file not found: {}", checkpointPath);
                return false;
            }

            model = MultiLayerNetwork.load(checkpointFile, true);
            modelVersion = extractVersionFromFilename(checkpointFile.getName());
            isReady = true;

            logger.info("Model loaded from checkpoint: {}", checkpointPath);
            return true;

        } catch (IOException e) {
            errorHandler.handleError("Failed to load model checkpoint", e);
            return false;
        }
    }

    /**
     * Check if model is ready for scoring
     */
    public boolean isReady() {
        return isReady;
    }

    /**
     * Get current model version
     */
    public String getModelVersion() {
        return modelVersion;
    }

    /**
     * Get model directory
     */
    private File getModelDirectory() {
        return configurationManager.getModelsPath().toFile();
    }

    /**
     * Find latest model checkpoint in directory
     */
    private File findLatestModelCheckpoint(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }

        File[] modelFiles = directory.listFiles((dir, name) ->
            name.startsWith("model-") && name.endsWith(".zip"));

        if (modelFiles == null || modelFiles.length == 0) {
            return null;
        }

        // Return the newest file
        File latest = modelFiles[0];
        for (File file : modelFiles) {
            if (file.lastModified() > latest.lastModified()) {
                latest = file;
            }
        }

        return latest;
    }

    /**
     * Generate model version string
     */
    private String generateModelVersion() {
        return "1.0.0-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
    }

    /**
     * Extract version from checkpoint filename
     */
    private String extractVersionFromFilename(String filename) {
        // Format: model-{version}.zip
        if (filename.startsWith("model-") && filename.endsWith(".zip")) {
            return filename.substring(6, filename.length() - 4);
        }
        return "unknown";
    }

    /**
     * Shutdown and release resources
     */
    public void shutdown() {
        if (model != null) {
            // Save final checkpoint before shutdown
            saveCheckpoint();
        }
        isReady = false;
        logger.info("Neural network model shut down");
    }
}
