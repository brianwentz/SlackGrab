# Neural Network Interface Contract

## Overview
This contract defines all interfaces for the neural network engine, including training, inference, and model management.

## Core Interfaces

### INeuralNetworkEngine

```java
public interface INeuralNetworkEngine {
    /**
     * Initialize the neural network with specified configuration
     * @param config Neural network configuration
     * @return true if initialization successful
     */
    boolean initialize(NeuralNetworkConfig config);

    /**
     * Score a message for importance
     * @param message The message to score
     * @param context Additional context (user behavior, channel info)
     * @return Importance score with confidence
     */
    ImportanceScore score(Message message, ScoringContext context);

    /**
     * Batch score multiple messages
     * @param messages List of messages to score
     * @param context Shared context for all messages
     * @return Map of message ID to importance score
     */
    Map<String, ImportanceScore> batchScore(List<Message> messages, ScoringContext context);

    /**
     * Get current model version
     * @return Model version information
     */
    ModelVersion getVersion();

    /**
     * Check if engine is ready for scoring
     * @return true if ready
     */
    boolean isReady();

    /**
     * Shutdown the engine and release resources
     */
    void shutdown();
}
```

### ITrainingPipeline

```java
public interface ITrainingPipeline {
    /**
     * Train the model with new feedback
     * @param feedback User feedback on message importance
     */
    void trainIncremental(Feedback feedback);

    /**
     * Batch train with multiple feedback items
     * @param feedbackList List of feedback items
     * @return Training result with metrics
     */
    TrainingResult batchTrain(List<Feedback> feedbackList);

    /**
     * Train from user interactions
     * @param interaction User interaction data
     */
    void learnFromInteraction(UserInteraction interaction);

    /**
     * Pause training (e.g., due to high CPU)
     */
    void pauseTraining();

    /**
     * Resume training
     */
    void resumeTraining();

    /**
     * Get current training status
     * @return Training status
     */
    TrainingStatus getStatus();

    /**
     * Save model checkpoint
     * @return Checkpoint ID
     */
    String saveCheckpoint();

    /**
     * Load model from checkpoint
     * @param checkpointId Checkpoint to load
     * @return true if successful
     */
    boolean loadCheckpoint(String checkpointId);
}
```

### IFeatureExtractor

```java
public interface IFeatureExtractor {
    /**
     * Extract features from a message
     * @param message The message to process
     * @param context Additional context
     * @return Feature vector
     */
    FeatureVector extractFeatures(Message message, MessageContext context);

    /**
     * Extract text embeddings
     * @param text The text to embed
     * @return Text embedding vector
     */
    float[] extractTextEmbedding(String text);

    /**
     * Extract temporal features
     * @param timestamp Message timestamp
     * @return Temporal feature vector
     */
    float[] extractTemporalFeatures(long timestamp);

    /**
     * Extract sender features
     * @param senderId The sender's ID
     * @param context Historical context for sender
     * @return Sender feature vector
     */
    float[] extractSenderFeatures(String senderId, SenderContext context);

    /**
     * Extract channel features
     * @param channelId The channel ID
     * @param context Channel statistics
     * @return Channel feature vector
     */
    float[] extractChannelFeatures(String channelId, ChannelContext context);

    /**
     * Get feature dimension
     * @return Total number of features
     */
    int getFeatureDimension();
}
```

### IGPUAccelerator

```java
public interface IGPUAccelerator {
    /**
     * Initialize GPU acceleration
     * @param config GPU configuration
     * @return true if GPU available and initialized
     */
    boolean initialize(GPUConfig config);

    /**
     * Check if GPU is available
     * @return GPU availability status
     */
    GPUStatus checkAvailability();

    /**
     * Get GPU memory usage
     * @return Memory usage in MB
     */
    MemoryUsage getMemoryUsage();

    /**
     * Set maximum GPU memory usage
     * @param percentage Maximum percentage (0.0 to 1.0)
     */
    void setMaxMemoryUsage(double percentage);

    /**
     * Execute computation on GPU
     * @param computation The computation to run
     * @return Computation result
     */
    <T> T executeOnGPU(GPUComputation<T> computation);

    /**
     * Fall back to CPU execution
     */
    void fallbackToCPU();

    /**
     * Get current execution mode
     * @return Current mode (GPU/CPU)
     */
    ExecutionMode getExecutionMode();

    /**
     * Release GPU resources
     */
    void release();
}
```

## Data Transfer Objects

### ImportanceScore

```java
public class ImportanceScore {
    private final ImportanceLevel level;  // HIGH, MEDIUM, LOW
    private final double confidence;      // 0.0 to 1.0
    private final double[] probabilities; // [high_prob, medium_prob, low_prob]
    private final long inferenceTimeMs;
    private final String modelVersion;

    // Constructor and getters
    public ImportanceScore(ImportanceLevel level, double confidence,
                          double[] probabilities, long inferenceTimeMs,
                          String modelVersion) {
        this.level = level;
        this.confidence = confidence;
        this.probabilities = probabilities;
        this.inferenceTimeMs = inferenceTimeMs;
        this.modelVersion = modelVersion;
    }

    // Getters...
}
```

### ScoringContext

```java
public class ScoringContext {
    private final Map<String, Double> senderImportance;
    private final Map<String, Double> channelImportance;
    private final UserBehaviorProfile userProfile;
    private final TimeWindow timeWindow;
    private final List<String> urgentKeywords;

    // Builder pattern for construction
    public static class Builder {
        // Builder implementation
    }
}
```

### Feedback

```java
public class Feedback {
    public enum FeedbackType {
        TOO_LOW,
        GOOD,
        TOO_HIGH
    }

    private final String messageId;
    private final FeedbackType type;
    private final ImportanceScore originalScore;
    private final long timestamp;
    private final String userId;
    private final FeatureVector features;

    // Constructor and methods
}
```

### TrainingResult

```java
public class TrainingResult {
    private final int samplesProcessed;
    private final double loss;
    private final double accuracy;
    private final long trainingTimeMs;
    private final Map<String, Double> metrics;
    private final boolean improved;

    // Constructor and getters
}
```

### FeatureVector

```java
public class FeatureVector {
    private final float[] values;
    private final Map<String, Integer> featureIndices;
    private final int dimension;

    public FeatureVector(float[] values, Map<String, Integer> featureIndices) {
        this.values = values;
        this.featureIndices = featureIndices;
        this.dimension = values.length;
    }

    public float getValue(String featureName) {
        Integer index = featureIndices.get(featureName);
        return index != null ? values[index] : 0.0f;
    }

    public INDArray toINDArray() {
        return Nd4j.create(values);
    }
}
```

## Configuration Objects

### NeuralNetworkConfig

```java
public class NeuralNetworkConfig {
    private final int inputDimension = 818;  // 768 text + 50 features
    private final int[] hiddenLayers = {512, 256};
    private final int outputDimension = 3;   // HIGH, MEDIUM, LOW
    private final double learningRate = 0.001;
    private final double dropout = 0.5;
    private final int batchSize = 32;
    private final String activation = "RELU";
    private final String optimizer = "ADAM";
    private final boolean useGPU = true;
    private final double maxGPUMemory = 0.8;

    // Builder pattern and validation
}
```

### GPUConfig

```java
public class GPUConfig {
    private final GPUType preferredType;  // INTEL, NVIDIA, AMD, ANY
    private final double maxMemoryUsage;  // 0.0 to 1.0
    private final boolean fallbackToCPU;
    private final int computeUnits;
    private final String openCLPlatform;

    // Constructor and getters
}
```

## Callback Interfaces

### ITrainingCallback

```java
public interface ITrainingCallback {
    void onEpochStart(int epoch);
    void onEpochEnd(int epoch, TrainingMetrics metrics);
    void onBatchProcessed(int batch, double loss);
    void onTrainingComplete(TrainingResult result);
    void onTrainingError(Exception error);
}
```

### IScoringCallback

```java
public interface IScoringCallback {
    void onScoringStart(String messageId);
    void onScoringComplete(String messageId, ImportanceScore score);
    void onScoringError(String messageId, Exception error);
    void onBatchScoringProgress(int processed, int total);
}
```

## Error Handling

### NeuralNetworkException

```java
public class NeuralNetworkException extends Exception {
    public enum ErrorType {
        INITIALIZATION_FAILED,
        MODEL_LOAD_ERROR,
        SCORING_ERROR,
        TRAINING_ERROR,
        GPU_ERROR,
        RESOURCE_EXHAUSTED
    }

    private final ErrorType errorType;
    private final String details;

    public NeuralNetworkException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.details = extractDetails(cause);
    }
}
```

## Service Contracts

### Model Persistence

```java
public interface IModelPersistence {
    void saveModel(MultiLayerNetwork model, String version);
    MultiLayerNetwork loadModel(String version);
    List<String> listAvailableVersions();
    void deleteOldVersions(int keepCount);
    ModelMetadata getModelMetadata(String version);
}
```

### Performance Monitoring

```java
public interface INeuralNetworkMonitor {
    void recordInferenceTime(long timeMs);
    void recordTrainingMetrics(TrainingMetrics metrics);
    void recordGPUUsage(double usage);
    void recordAccuracy(double accuracy);
    PerformanceReport generateReport();
}
```

## Thread Safety Requirements

All implementations must be thread-safe for:
- Concurrent scoring requests
- Simultaneous training and scoring
- Resource monitoring access
- Model checkpoint operations

## Performance Requirements

### Latency
- Single message scoring: < 1000ms (P99)
- Batch scoring (100 messages): < 5000ms
- Feature extraction: < 100ms per message
- Model loading: < 10 seconds

### Throughput
- Minimum: 100 messages/second
- Target: 500 messages/second
- Peak: 1000 messages/second (with GPU)

### Resource Limits
- Maximum RAM: 300MB for model
- Maximum GPU RAM: 80% of available
- CPU usage during scoring: < 10%
- CPU usage during training: < 50%

## Versioning Strategy

### Model Versioning
```
Format: {major}.{minor}.{patch}-{timestamp}
Example: 1.0.0-20240315120000
```

### API Versioning
- Backward compatibility maintained within major version
- Deprecation notices for 2 minor versions
- Breaking changes only in major versions

## Testing Requirements

### Unit Tests
- Mock GPU operations for CPU-only testing
- Test feature extraction independently
- Verify scoring accuracy with test dataset
- Test error handling paths

### Integration Tests
- End-to-end scoring pipeline
- Training with real feedback
- GPU/CPU fallback scenarios
- Model persistence and loading

### Performance Tests
- Load testing with 5000 messages/day
- Memory leak detection
- GPU memory management
- Training convergence validation

## Security Considerations

### Input Validation
- Maximum message length: 40000 characters
- Feature vector bounds checking
- Sanitize user feedback input
- Validate model files before loading

### Model Security
- Sign model files with checksum
- Encrypt sensitive features in memory
- No PII in training logs
- Secure model storage location