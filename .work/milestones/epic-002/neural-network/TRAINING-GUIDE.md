# Neural Network Training Guide

## Overview

This guide explains how the SlackGrab neural network learns and improves over time through continuous user interaction and feedback.

---

## Training Lifecycle

### Phase 1: Initialization (First Run)

**State:** No trained model exists

**Behavior:**
- Neural network initialized with random weights
- All messages scored at default (0.5, MEDIUM importance)
- Model begins learning from first user interaction

**Duration:** Minutes to hours

**User Experience:**
- Initial predictions will be inaccurate (expected)
- No action required from user
- System learns passively from usage

**Code:**
```java
NeuralNetworkModel model = new NeuralNetworkModel(...);
model.initialize(); // Creates new model if none exists

ImportanceScore score = scorer.score(message);
// Returns: score=0.5, level=MEDIUM, confidence=0.0 (until training begins)
```

---

### Phase 2: Initial Learning (Days 1-7)

**State:** Accumulating user interactions

**Learning Sources:**
1. **Implicit Interactions:**
   - Messages viewed > 2 seconds → Positive signal
   - Messages replied to → Strong positive
   - Messages with reactions → Moderate positive
   - Messages ignored → Negative signal

2. **Explicit Feedback:**
   - User marks prediction as TOO_LOW → Increase importance
   - User marks prediction as TOO_HIGH → Decrease importance
   - User marks prediction as GOOD → Reinforce learning

**Training Schedule:**
- **Online Training:** Continuous (every interaction)
- **Batch Training:** After 1000 interactions or daily
- **Checkpoints:** Every 100 online examples

**Expected Progress:**
- Day 1: 50-100 interactions → Basic patterns emerging
- Day 3: 300-500 interactions → Noticeable improvement
- Day 7: 1000+ interactions → Good accuracy

**User Experience:**
- Predictions improve gradually
- More confident scores (higher confidence values)
- Better alignment with user preferences

---

### Phase 3: Stable Operation (Week 2+)

**State:** Model well-trained on user patterns

**Performance:**
- Accuracy: 70-80% (measured by GOOD feedback ratio)
- Confidence: 0.7-0.9 for most predictions
- Latency: < 100ms per message

**Maintenance:**
- Continuous online learning adapts to changes
- Periodic batch training reinforces stability
- Old data (> 30 days) cleaned up

**User Experience:**
- Accurate importance predictions
- Minimal manual correction needed
- System "understands" user preferences

---

## Training Data Sources

### 1. User Interactions (Primary)

**Tracked Automatically:**

| Interaction Type | Importance Signal | Weight |
|-----------------|-------------------|--------|
| Read (2-5s) | Low positive | 0.4 |
| Read (5-10s) | Medium positive | 0.6 |
| Read (>10s) | High positive | 0.9 |
| Reply | Very high positive | 0.9 |
| Reaction | Medium positive | 0.6 |
| No interaction | Negative | 0.2 |

**Code:**
```java
// When user views message
interactionRepository.recordInteraction(
    messageId,
    "READ",
    dwellTimeMs
);

// Converted to training example
TrainingExample example = TrainingExample.fromInteraction(
    features,
    interacted = true,
    dwellTime = 8000 // 8 seconds
);
// Target score: 0.6 (medium importance)

// Enqueued for online training
onlineTrainer.enqueueExample(example);
```

### 2. Explicit Feedback (Secondary)

**User Provides Feedback:**

| Feedback Type | Adjustment | Target Score |
|--------------|------------|--------------|
| TOO_LOW | Increase by 0.3 | original + 0.3 |
| GOOD | No change | original |
| TOO_HIGH | Decrease by 0.3 | original - 0.3 |

**Code:**
```java
// User provides feedback
feedbackRepository.recordFeedback(
    messageId,
    FeedbackType.TOO_LOW,
    originalScore = 0.4
);

// Converted to training example
TrainingExample example = TrainingExample.fromFeedback(
    features,
    FeedbackType.TOO_LOW,
    originalScore = 0.4
);
// Target score: 0.7 (increased by 0.3)

// Immediately trained
onlineTrainer.enqueueExample(example);
```

---

## Training Modes

### Online Training (Continuous)

**How It Works:**
1. User interaction occurs
2. Training example created
3. Added to non-blocking queue (max 1000 examples)
4. Background thread processes queue
5. Model updated incrementally (one example at a time)
6. Checkpoint saved every 100 examples

**Characteristics:**
- **Speed:** Immediate (next example)
- **Learning Rate:** 0.001 (slow, stable)
- **Batch Size:** 1 (single example)
- **Duration:** Milliseconds per example
- **Resource Usage:** < 5% CPU

**When Used:**
- Every user interaction
- Every explicit feedback
- Continuous adaptation

**Code Flow:**
```java
// User interaction
UserInteraction interaction = ...;

// Create training example
TrainingExample example = TrainingExample.fromInteraction(...);

// Enqueue (non-blocking)
onlineTrainer.enqueueExample(example);

// Background thread processes
while (isRunning) {
    TrainingExample ex = queue.poll();
    if (ex != null) {
        neuralNetwork.trainOnline(ex);
        checkpointIfNeeded();
    }
}
```

**Advantages:**
- Fast adaptation to new patterns
- Always learning from latest interactions
- No batch delays

**Disadvantages:**
- Can be unstable with outliers
- May overfit to recent examples
- Needs batch training for stability

---

### Batch Training (Periodic)

**How It Works:**
1. Triggered after 1000 interactions or daily
2. Collect recent training examples (last 1000)
3. Train for multiple epochs (default: 5)
4. Higher learning rate (0.01)
5. Save checkpoint after completion

**Characteristics:**
- **Speed:** Slower (minutes)
- **Learning Rate:** 0.01 (10x online)
- **Batch Size:** 32-1000 examples
- **Epochs:** 5
- **Duration:** 2-10 minutes
- **Resource Usage:** < 50% CPU

**When Used:**
- After 1000 online examples
- Every 24 hours
- Manual trigger (if needed)

**Code Flow:**
```java
// Triggered by scheduler
void scheduledBatchTraining() {
    // Check if enough examples
    if (examplesTrained < 1000) return;

    // Collect examples
    List<TrainingExample> examples =
        collectRecentExamples(1000);

    // Train batch
    BatchTrainer.TrainingResult result =
        batchTrainer.trainBatch(examples, epochs=5);

    // Log result
    logger.info("Batch training: {}", result);
}
```

**Advantages:**
- Stable convergence
- Learns broader patterns
- Corrects online training drift

**Disadvantages:**
- Slower feedback loop
- Higher resource usage
- Requires sufficient examples

---

## Resource Management

### CPU Usage Limits

| Scenario | CPU Limit | Behavior |
|----------|-----------|----------|
| GPU Active | 5% | Training pauses if exceeded |
| CPU-Only | 20% | Training pauses if exceeded |
| Batch Training | 50% | Temporary spike allowed |

**Auto-Pause Logic:**
```java
// Checked every training iteration
if (resourceMonitor.shouldPauseTraining()) {
    onlineTrainer.pause();
    // Wait for resources
    Thread.sleep(1000);
} else if (isPaused) {
    onlineTrainer.resume();
}
```

### Memory Limits

| Component | Memory Usage |
|-----------|--------------|
| Model weights | ~50 MB |
| Training queue | ~50 MB |
| Feature cache | ~10 MB |
| **Total** | **~110 MB** |
| **Limit** | **4 GB** |

### GPU Resource Limits

| Resource | Limit | Behavior |
|----------|-------|----------|
| GPU RAM | 80% | Fallback to CPU if exceeded |
| GPU Compute | Auto | Dynamic based on availability |

---

## Model Checkpointing

### Checkpoint Schedule

| Event | Frequency | Location |
|-------|-----------|----------|
| Online training | Every 100 examples | `%LOCALAPPDATA%\SlackGrab\models\` |
| Batch training | After completion | Same |
| Manual save | On demand | Same |
| Shutdown | Always | Same |

### Checkpoint Format

**Filename:** `model-{version}.zip`
- Example: `model-1.0.0-20250315120530.zip`

**Version Format:** `{major}.{minor}.{patch}-{timestamp}`
- Major: Breaking architecture changes
- Minor: Compatible improvements
- Patch: Bug fixes
- Timestamp: yyyyMMddHHmmss

**Contents:**
- Neural network weights (DL4J format)
- Model configuration
- Training metadata

**Loading:**
```java
// Automatic on initialization
model.initialize();
// Loads latest checkpoint if exists
// Otherwise creates new random model

// Manual load
model.loadCheckpoint("/path/to/model-1.0.0-20250315120530.zip");
```

---

## Monitoring Training Progress

### Training Statistics

**Available via API:**
```java
OnlineTrainer.TrainingStats stats = onlineTrainer.getStats();
System.out.println(stats);
// Output: TrainingStats[trained=523, queued=5, paused=false, running=true]
```

**Logged Automatically:**
```
[INFO] Online training: 100 examples trained
[INFO] Model checkpoint saved: model-1.0.0-20250315120530.zip
[INFO] Batch training: 1000 examples, 5 epochs, 3200ms
[INFO] Training accuracy: 72.4% (724/1000 marked GOOD)
```

### Performance Metrics

**Tracked Internally:**
1. **Examples Trained:** Total count
2. **Training Loss:** MSE per batch
3. **Inference Time:** Latency per score
4. **Feedback Accuracy:** % GOOD feedback
5. **Resource Usage:** CPU/GPU/Memory

**Not Exposed to User** (Silent operation principle)

---

## Training Best Practices

### For Developers

1. **Never Block on Training:**
   ```java
   // Good: Non-blocking
   onlineTrainer.enqueueExample(example);

   // Bad: Blocking
   neuralNetwork.trainOnline(example); // Blocks caller
   ```

2. **Always Check Resources:**
   ```java
   if (resourceMonitor.isWithinLimits()) {
       // Safe to train
   } else {
       // Defer training
   }
   ```

3. **Handle Training Failures Silently:**
   ```java
   try {
       onlineTrainer.enqueueExample(example);
   } catch (Exception e) {
       errorHandler.handleError("Training failed", e);
       // Continue normally - never interrupt user
   }
   ```

4. **Use Context for Better Features:**
   ```java
   ScoringContext context = new ScoringContext.Builder()
       .withSenderImportance(senderMap)
       .withChannelImportance(channelMap)
       .withUrgentKeywords(keywords)
       .build();

   ImportanceScore score = scorer.score(message, context);
   ```

### For Users (Automatic)

**No Manual Training Required** ✅

The system learns automatically from:
- Reading messages (dwell time)
- Replying to messages
- Adding reactions
- Providing explicit feedback (optional)

**To Accelerate Learning:**
1. Use Slack normally
2. Optionally provide feedback on predictions
3. Engage with important messages
4. Ignore unimportant messages

That's it! The system handles everything else.

---

## Troubleshooting

### Problem: Predictions Not Improving

**Possible Causes:**
1. Insufficient training data (< 100 interactions)
2. Training paused due to resource limits
3. Model checkpoint corrupted

**Solutions:**
```java
// Check training stats
TrainingStats stats = onlineTrainer.getStats();
if (stats.examplesTrained() < 100) {
    // Need more interactions - wait
}

if (stats.paused()) {
    // Check resources
    ResourceUsage usage = resourceMonitor.getCurrentUsage();
    logger.info("Resources: {}", usage);
}

// Force batch training
trainingScheduler.triggerBatchTraining();
```

### Problem: High CPU Usage

**Cause:** Training not respecting limits

**Solution:**
```java
// Verify resource monitor
if (!resourceMonitor.isWithinLimits()) {
    onlineTrainer.pause();
}

// Lower limits
resourceMonitor.setMaxCpuUsage(0.10); // 10% instead of 20%
```

### Problem: Model Checkpoint Failed

**Cause:** Disk space or permissions

**Solution:**
```java
// Check model directory
File modelDir = configurationManager.getDataPath()
    .resolve("models")
    .toFile();

if (!modelDir.exists()) {
    modelDir.mkdirs();
}

if (!modelDir.canWrite()) {
    logger.error("Cannot write to model directory");
}
```

---

## Advanced: Manual Training

### Trigger Batch Training

```java
// Via scheduler
TrainingScheduler scheduler = ...;
scheduler.triggerBatchTraining();

// Direct
BatchTrainer batchTrainer = ...;
List<TrainingExample> examples = collectExamples();
BatchTrainer.TrainingResult result =
    batchTrainer.trainBatch(examples, epochs=10);
```

### Create Custom Training Examples

```java
// From message and ground truth
SlackMessage message = ...;
FeatureVector features = featureExtractor.extractFeatures(message, context);

TrainingExample example = new TrainingExample(
    features,
    targetScore = 0.8,  // Ground truth
    targetLevel = ImportanceLevel.HIGH,
    timestamp = System.currentTimeMillis()
);

onlineTrainer.enqueueExample(example);
```

### Export Model

```java
// Save current model
String checkpointPath = neuralNetwork.saveCheckpoint();
System.out.println("Model saved: " + checkpointPath);

// Copy to backup location
Files.copy(
    Path.of(checkpointPath),
    Path.of("backup/model.zip")
);
```

---

## Conclusion

The SlackGrab neural network training system:

✅ **Automatic:** No user configuration needed
✅ **Continuous:** Always learning from interactions
✅ **Adaptive:** Adjusts to changing preferences
✅ **Efficient:** Resource-aware training
✅ **Reliable:** Checkpointing and error recovery

**Key Takeaways:**
1. Model improves gradually over 1-2 weeks
2. Both implicit and explicit feedback used
3. Online + batch training for stability
4. Resources monitored and limited
5. Silent operation - no user interruption

For more details, see:
- `INTERFACE.md` - Complete API documentation
- `EVIDENCE.md` - Implementation details
- Architecture docs in `.work/foundation/architecture/`
