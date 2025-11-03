# Neural Network Interface Documentation

## Overview

This document defines the public API interfaces for the SlackGrab neural network ML engine implementing Epic 2: Neural Network Learning (Weeks 3-4).

## Core Components

### ImportanceScorer

**Package:** `com.slackgrab.ml`

**Purpose:** Main entry point for message importance scoring

**Public Methods:**

```java
// Initialize the scorer
boolean initialize()

// Score a single message
ImportanceScore score(SlackMessage message)
ImportanceScore score(SlackMessage message, ScoringContext context)

// Batch score multiple messages
ImportanceScore[] batchScore(SlackMessage[] messages)
ImportanceScore[] batchScore(SlackMessage[] messages, ScoringContext context)

// Check if ready
boolean isReady()

// Get model version
String getModelVersion()

// Shutdown
void shutdown()
```

**Usage Example:**

```java
ImportanceScorer scorer = injector.getInstance(ImportanceScorer.class);
scorer.initialize();

SlackMessage message = messageRepository.getMessage("12345").orElseThrow();
ImportanceScore score = scorer.score(message);

System.out.println("Importance: " + score.level());
System.out.println("Score: " + score.score());
System.out.println("Confidence: " + score.confidence());
```

---

### FeatureExtractor

**Package:** `com.slackgrab.ml.features`

**Purpose:** Extract features from messages for neural network input

**Public Methods:**

```java
// Extract all features from a message
FeatureVector extractFeatures(SlackMessage message, ScoringContext context)

// Get feature dimension
int getFeatureDimension()

// Get feature indices
Map<String, Integer> getFeatureIndices()
```

**Features Extracted (25 total):**

1. **Text Features (10):**
   - Text length (normalized)
   - Word count (normalized)
   - Has question marks
   - Has URLs
   - Has mentions
   - Has emojis
   - Uppercase ratio
   - Exclamation count
   - Average word length
   - Urgent keyword match

2. **User Features (5):**
   - Sender importance
   - Sender frequency
   - User interaction rate
   - Sender average importance
   - Is bot

3. **Media Features (3):**
   - Has attachments
   - Attachment count
   - In thread

4. **Temporal Features (5):**
   - Hour of day
   - Day of week
   - Is business hours
   - Recency
   - Is weekend

5. **Channel Features (2):**
   - Channel importance
   - Is private channel

---

### NeuralNetworkModel

**Package:** `com.slackgrab.ml.model`

**Purpose:** Deep learning model for importance scoring

**Architecture:**
- Input Layer: 25 features
- Hidden Layer 1: 64 neurons, ReLU, 20% dropout
- Hidden Layer 2: 32 neurons, ReLU, 20% dropout
- Output Layer: 1 neuron, Sigmoid (0.0-1.0)

**Training:**
- Loss: MSE (Mean Squared Error)
- Optimizer: Adam
- Learning Rate: 0.001 (online), 0.01 (batch)

**Public Methods:**

```java
// Initialize model
boolean initialize()

// Score features
double score(FeatureVector features)
double[] batchScore(FeatureVector[] features)

// Training
void trainOnline(TrainingExample example)
void trainBatch(TrainingExample[] examples, int epochs)

// Persistence
String saveCheckpoint()
boolean loadCheckpoint(String checkpointPath)

// Status
boolean isReady()
String getModelVersion()
void shutdown()
```

---

### TrainingScheduler

**Package:** `com.slackgrab.ml.training`

**Purpose:** Coordinate background training operations

**Implements:** `ManagedService`

**Public Methods:**

```java
// Service lifecycle
void start() throws Exception
void stop() throws Exception

// Training control
void triggerBatchTraining()
boolean isRunning()

// Statistics
OnlineTrainer.TrainingStats getOnlineStats()
```

**Training Schedule:**
- **Online Training:** Continuous, real-time updates
- **Batch Training:** Every 1000 interactions or daily
- **Checkpointing:** Every 100 online examples

---

### OnlineTrainer

**Package:** `com.slackgrab.ml.training`

**Purpose:** Incremental learning from user interactions

**Public Methods:**

```java
// Control
void start()
void stop()
void pause()
void resume()

// Training
boolean enqueueExample(TrainingExample example)

// Status
int getQueueSize()
int getExamplesTrained()
boolean isPaused()
TrainingStats getStats()
```

---

### BatchTrainer

**Package:** `com.slackgrab.ml.training`

**Purpose:** Periodic full retraining

**Public Methods:**

```java
// Training
TrainingResult trainBatch(List<TrainingExample> examples)
TrainingResult trainBatch(List<TrainingExample> examples, int epochs)

// Status
boolean isTraining()
```

---

### GpuAccelerator

**Package:** `com.slackgrab.ml.gpu`

**Purpose:** GPU acceleration with CPU fallback

**Public Methods:**

```java
// Initialization
boolean initialize()

// Status
ExecutionMode getExecutionMode()
GpuStatus getStatus()
MemoryUsage getMemoryUsage()

// Configuration
void setMaxMemoryUsage(double percentage)

// Control
void fallbackToCPU()
boolean tryEnableGPU()
boolean isMemoryWithinLimits()

// Cleanup
void shutdown()
```

---

### ResourceMonitor

**Package:** `com.slackgrab.ml.gpu`

**Purpose:** Monitor CPU, GPU, and memory usage

**Public Methods:**

```java
// Monitoring
ResourceUsage getCurrentUsage()
boolean isWithinLimits()
boolean shouldPauseTraining()
void logResourceUsage()
```

**Resource Limits:**
- GPU: 80% max RAM
- CPU: 5% (with GPU), 20% (CPU-only)
- Memory: 4GB total

---

## Data Models

### ImportanceScore

```java
public record ImportanceScore(
    ImportanceLevel level,      // HIGH, MEDIUM, LOW
    double score,               // 0.0-1.0
    double confidence,          // 0.0-1.0
    double[] probabilities,     // [P(high), P(medium), P(low)]
    long inferenceTimeMs,       // Scoring time
    String modelVersion         // Model version
)
```

### FeatureVector

```java
public class FeatureVector {
    float[] getValues()
    float getValue(String featureName)
    float getValue(int index)
    int getDimension()
    INDArray toINDArray()
    INDArray toBatchINDArray()
}
```

### TrainingExample

```java
public record TrainingExample(
    FeatureVector features,
    double targetScore,
    ImportanceLevel targetLevel,
    long timestamp
)
```

### ScoringContext

```java
public class ScoringContext {
    double getSenderImportance(String senderId)
    double getChannelImportance(String channelId)
    List<String> getUrgentKeywords()
    long getCurrentTime()

    // Builder pattern
    static class Builder { ... }
}
```

---

## Repositories

### InteractionRepository

**Package:** `com.slackgrab.data`

**Purpose:** Track user interactions for learning

**Public Methods:**

```java
// Recording
boolean recordInteraction(String messageId, String interactionType, Long readingTimeMs)

// Querying
List<UserInteraction> getMessageInteractions(String messageId)
List<UserInteraction> getRecentInteractions(int limit)
int getInteractionCount(String messageId)
int getTotalInteractionCount()

// Cleanup
int deleteOldInteractions(int days)
```

**Interaction Types:**
- `READ` - Message viewed > 2 seconds
- `REPLY` - User replied to message
- `REACTION` - User added emoji reaction
- `THREAD` - User participated in thread

---

### FeedbackRepository

**Package:** `com.slackgrab.data`

**Purpose:** Store explicit user feedback

**Public Methods:**

```java
// Recording
long recordFeedback(String messageId, FeedbackType feedbackType, double originalScore)

// Querying
List<Feedback> getMessageFeedback(String messageId)
List<Feedback> getRecentFeedback(int limit)
Optional<Feedback> getFeedbackById(long feedbackId)
FeedbackStats getStats()

// Management
boolean deleteFeedback(long feedbackId)  // For undo
int deleteOldFeedback(int days)
```

**Feedback Types:**
- `TOO_LOW` - Model scored too low
- `GOOD` - Score was appropriate
- `TOO_HIGH` - Model scored too high

---

## Integration Points

### Guice Module

```java
// MLModule.java
public class MLModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ImportanceScorer.class);
        bind(NeuralNetworkModel.class);
        bind(FeatureExtractor.class);
        bind(OnlineTrainer.class);
        bind(BatchTrainer.class);
        bind(TrainingScheduler.class);
        bind(GpuAccelerator.class);
        bind(ResourceMonitor.class);
    }
}
```

### Service Lifecycle

```java
// In SlackGrabApplication
injector.getInstance(TrainingScheduler.class).start();
injector.getInstance(ImportanceScorer.class).initialize();

// On shutdown
trainingScheduler.stop();
importanceScorer.shutdown();
```

---

## Performance Targets

### Latency
- Single message scoring: < 1000ms (P99)
- Batch scoring (100 messages): < 5000ms
- Feature extraction: < 100ms per message

### Throughput
- Minimum: 100 messages/second
- Target: 500 messages/second
- Peak: 1000 messages/second (with GPU)

### Resource Limits
- Maximum RAM: 4GB total
- Maximum GPU RAM: 80% of available
- CPU usage: 5% (with GPU), 20% (CPU-only)

---

## Error Handling

All ML components follow silent error handling:
- Errors logged but not thrown
- Graceful degradation (default scores on failure)
- Automatic recovery where possible
- No user-facing error messages

Example:
```java
try {
    ImportanceScore score = scorer.score(message);
} catch (Exception e) {
    // Never happens - scorer returns default on error
}
```

---

## Thread Safety

All public APIs are thread-safe:
- Concurrent scoring requests supported
- Training and scoring can run simultaneously
- Resource monitoring is synchronized
- Model checkpointing is atomic

---

## Version Compatibility

**Model Versioning Format:**
```
{major}.{minor}.{patch}-{timestamp}
Example: 1.0.0-20250315120000
```

**API Versioning:**
- Backward compatibility within major version
- Breaking changes only in major versions
- Deprecation notices for 2 minor versions
