# Feedback System Interface Contract

## Overview
This contract defines all interfaces for the three-level feedback system, including feedback collection, processing, and model adaptation.

## Core Interfaces

### IFeedbackProcessor

```java
public interface IFeedbackProcessor {
    /**
     * Process single feedback item
     * @param feedback User feedback
     * @return Processing result
     */
    CompletableFuture<FeedbackResult> processFeedback(Feedback feedback);

    /**
     * Process batch feedback
     * @param feedbackList List of feedback items
     * @return Batch processing result
     */
    CompletableFuture<BatchFeedbackResult> processBatchFeedback(
        List<Feedback> feedbackList
    );

    /**
     * Undo last feedback
     * @param userId User who gave the feedback
     * @return Undo result
     */
    CompletableFuture<UndoResult> undoLastFeedback(String userId);

    /**
     * Undo specific feedback
     * @param feedbackId Feedback ID to undo
     * @return Undo result
     */
    CompletableFuture<UndoResult> undoFeedback(long feedbackId);

    /**
     * Get feedback history for user
     * @param userId User ID
     * @param limit Maximum items to return
     * @return Feedback history
     */
    List<FeedbackHistoryItem> getFeedbackHistory(String userId, int limit);

    /**
     * Get feedback statistics
     * @param userId User ID
     * @return Feedback statistics
     */
    FeedbackStatistics getStatistics(String userId);
}
```

### IBatchFeedbackManager

```java
public interface IBatchFeedbackManager {
    /**
     * Start batch feedback session
     * @param userId User starting the batch
     * @return Session ID
     */
    String startBatchSession(String userId);

    /**
     * Add feedback to batch session
     * @param sessionId Session ID
     * @param feedback Feedback to add
     */
    void addToBatch(String sessionId, Feedback feedback);

    /**
     * Submit batch session
     * @param sessionId Session ID
     * @return Batch submission result
     */
    CompletableFuture<BatchFeedbackResult> submitBatch(String sessionId);

    /**
     * Cancel batch session
     * @param sessionId Session ID
     */
    void cancelBatch(String sessionId);

    /**
     * Get batch session status
     * @param sessionId Session ID
     * @return Session status
     */
    BatchSessionStatus getBatchStatus(String sessionId);

    /**
     * Get active sessions for user
     * @param userId User ID
     * @return List of active session IDs
     */
    List<String> getActiveSessions(String userId);
}
```

### IUndoManager

```java
public interface IUndoManager {
    /**
     * Register feedback for undo capability
     * @param feedback Feedback to register
     * @return Undo token
     */
    String registerFeedback(Feedback feedback);

    /**
     * Undo feedback by token
     * @param undoToken Undo token
     * @return Undo result
     */
    UndoResult undoByToken(String undoToken);

    /**
     * Get undo stack for user
     * @param userId User ID
     * @param limit Maximum items to return
     * @return Undoable feedback items
     */
    List<UndoableItem> getUndoStack(String userId, int limit);

    /**
     * Clear undo history for user
     * @param userId User ID
     * @param olderThanDays Clear items older than this many days
     */
    void clearHistory(String userId, int olderThanDays);

    /**
     * Check if feedback can be undone
     * @param feedbackId Feedback ID
     * @return true if can be undone
     */
    boolean canUndo(long feedbackId);
}
```

### ITrainingDataGenerator

```java
public interface ITrainingDataGenerator {
    /**
     * Generate training sample from feedback
     * @param feedback User feedback
     * @return Training sample
     */
    TrainingSample generateSample(Feedback feedback);

    /**
     * Generate training samples from batch
     * @param feedbackList List of feedback items
     * @return List of training samples
     */
    List<TrainingSample> generateBatchSamples(List<Feedback> feedbackList);

    /**
     * Adjust label based on feedback type
     * @param originalScore Original importance score
     * @param feedbackType Feedback type
     * @return Adjusted label
     */
    ImportanceLevel adjustLabel(ImportanceScore originalScore, FeedbackType feedbackType);

    /**
     * Calculate sample weight
     * @param feedback Feedback item
     * @return Sample weight (higher = more important)
     */
    double calculateSampleWeight(Feedback feedback);

    /**
     * Validate training sample
     * @param sample Training sample
     * @return Validation result
     */
    ValidationResult validateSample(TrainingSample sample);
}
```

### IModelUpdater

```java
public interface IModelUpdater {
    /**
     * Update model with feedback
     * @param feedback User feedback
     * @return Update result
     */
    CompletableFuture<ModelUpdateResult> updateFromFeedback(Feedback feedback);

    /**
     * Batch update model
     * @param feedbackList List of feedback items
     * @return Batch update result
     */
    CompletableFuture<ModelUpdateResult> batchUpdateFromFeedback(
        List<Feedback> feedbackList
    );

    /**
     * Trigger incremental training
     * @return Training result
     */
    CompletableFuture<TrainingResult> triggerTraining();

    /**
     * Get update status
     * @return Current update status
     */
    UpdateStatus getUpdateStatus();

    /**
     * Pause model updates
     */
    void pauseUpdates();

    /**
     * Resume model updates
     */
    void resumeUpdates();
}
```

## Data Transfer Objects

### Feedback

```java
public class Feedback {
    public enum FeedbackType {
        TOO_LOW("too_low"),
        GOOD("good"),
        TOO_HIGH("too_high");

        private final String value;
        FeedbackType(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    private Long id;
    private String messageId;
    private String userId;
    private FeedbackType type;
    private ImportanceScore originalScore;
    private long timestamp;
    private String sessionId;
    private Map<String, Object> context;
    private boolean processed;
    private boolean undone;

    // Constructor and getters/setters
    public Feedback(String messageId, String userId, FeedbackType type,
                   ImportanceScore originalScore) {
        this.messageId = messageId;
        this.userId = userId;
        this.type = type;
        this.originalScore = originalScore;
        this.timestamp = System.currentTimeMillis();
        this.processed = false;
        this.undone = false;
    }

    public ImportanceLevel getTargetLevel() {
        switch (type) {
            case TOO_LOW:
                return originalScore.getLevel() == ImportanceLevel.LOW
                    ? ImportanceLevel.MEDIUM
                    : ImportanceLevel.HIGH;
            case TOO_HIGH:
                return originalScore.getLevel() == ImportanceLevel.HIGH
                    ? ImportanceLevel.MEDIUM
                    : ImportanceLevel.LOW;
            case GOOD:
                return originalScore.getLevel();
            default:
                throw new IllegalStateException("Unknown feedback type");
        }
    }
}
```

### FeedbackResult

```java
public class FeedbackResult {
    private final boolean success;
    private final String feedbackId;
    private final String message;
    private final ImportanceLevel originalLevel;
    private final ImportanceLevel targetLevel;
    private final boolean modelUpdated;
    private final long processingTimeMs;
    private final String undoToken;

    public FeedbackResult(boolean success, String feedbackId, String message,
                         ImportanceLevel originalLevel, ImportanceLevel targetLevel,
                         boolean modelUpdated, long processingTimeMs, String undoToken) {
        this.success = success;
        this.feedbackId = feedbackId;
        this.message = message;
        this.originalLevel = originalLevel;
        this.targetLevel = targetLevel;
        this.modelUpdated = modelUpdated;
        this.processingTimeMs = processingTimeMs;
        this.undoToken = undoToken;
    }

    // Getters...
}
```

### BatchFeedbackResult

```java
public class BatchFeedbackResult {
    private final String sessionId;
    private final int totalFeedback;
    private final int successful;
    private final int failed;
    private final List<FeedbackResult> results;
    private final boolean modelUpdated;
    private final long totalProcessingTimeMs;
    private final Map<FeedbackType, Integer> countsByType;

    // Constructor and getters...

    public double getSuccessRate() {
        return totalFeedback > 0 ? (double) successful / totalFeedback : 0.0;
    }
}
```

### UndoResult

```java
public class UndoResult {
    private final boolean success;
    private final String message;
    private final Long feedbackId;
    private final FeedbackType originalFeedbackType;
    private final boolean modelReverted;
    private final long processingTimeMs;

    public UndoResult(boolean success, String message, Long feedbackId,
                     FeedbackType originalFeedbackType, boolean modelReverted,
                     long processingTimeMs) {
        this.success = success;
        this.message = message;
        this.feedbackId = feedbackId;
        this.originalFeedbackType = originalFeedbackType;
        this.modelReverted = modelReverted;
        this.processingTimeMs = processingTimeMs;
    }

    // Getters...
}
```

### FeedbackHistoryItem

```java
public class FeedbackHistoryItem {
    private final Long feedbackId;
    private final String messageId;
    private final FeedbackType type;
    private final ImportanceLevel originalLevel;
    private final ImportanceLevel targetLevel;
    private final long timestamp;
    private final boolean undone;
    private final String undoToken;

    // Constructor and getters...
}
```

### FeedbackStatistics

```java
public class FeedbackStatistics {
    private final String userId;
    private final long totalFeedback;
    private final Map<FeedbackType, Long> countsByType;
    private final Map<ImportanceLevel, Long> countsByOriginalLevel;
    private final double averageConfidenceWhenCorrect;
    private final double averageConfidenceWhenIncorrect;
    private final int feedbackLast24Hours;
    private final int feedbackLast7Days;
    private final int feedbackLast30Days;
    private final List<FeedbackTrend> trends;

    // Constructor and getters...

    public double getCorrectPercentage() {
        Long goodCount = countsByType.getOrDefault(FeedbackType.GOOD, 0L);
        return totalFeedback > 0 ? (double) goodCount / totalFeedback * 100 : 0.0;
    }

    public double getIncorrectPercentage() {
        return 100.0 - getCorrectPercentage();
    }
}
```

### BatchSessionStatus

```java
public class BatchSessionStatus {
    public enum Status {
        ACTIVE,
        SUBMITTED,
        CANCELLED,
        EXPIRED
    }

    private final String sessionId;
    private final String userId;
    private final Status status;
    private final int feedbackCount;
    private final long createdAt;
    private final long expiresAt;
    private final Map<FeedbackType, Integer> countsByType;

    // Constructor and getters...

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isActive() {
        return status == Status.ACTIVE && !isExpired();
    }
}
```

### UndoableItem

```java
public class UndoableItem {
    private final Long feedbackId;
    private final String messageId;
    private final String messageText;
    private final FeedbackType type;
    private final long timestamp;
    private final String undoToken;
    private final boolean canUndo;
    private final String reason;

    // Constructor and getters...
}
```

### ModelUpdateResult

```java
public class ModelUpdateResult {
    private final boolean success;
    private final int samplesAdded;
    private final boolean trainingTriggered;
    private final double accuracyBefore;
    private final double accuracyAfter;
    private final long updateTimeMs;
    private final String newModelVersion;

    // Constructor and getters...

    public double getAccuracyImprovement() {
        return accuracyAfter - accuracyBefore;
    }
}
```

## Callback Interfaces

### IFeedbackCallback

```java
public interface IFeedbackCallback {
    void onFeedbackReceived(Feedback feedback);
    void onFeedbackProcessed(Feedback feedback, FeedbackResult result);
    void onFeedbackError(Feedback feedback, Exception error);
    void onBatchComplete(BatchFeedbackResult result);
    void onModelUpdated(ModelUpdateResult result);
}
```

### IUndoCallback

```java
public interface IUndoCallback {
    void onUndoRequested(Long feedbackId);
    void onUndoComplete(UndoResult result);
    void onUndoError(Long feedbackId, Exception error);
}
```

## Configuration

### FeedbackConfig

```java
public class FeedbackConfig {
    // Processing settings
    private final boolean asyncProcessing = true;
    private final int batchProcessingThreshold = 10;
    private final long batchTimeoutMs = 5000;

    // Undo settings
    private final int maxUndoHistory = 100;
    private final long undoExpirationDays = 7;
    private final boolean allowUndoAfterTraining = true;

    // Model update settings
    private final boolean autoTriggerTraining = true;
    private final int trainingTriggerThreshold = 32;
    private final long minTimeBetweenUpdatesMs = 60000;

    // Session settings
    private final long batchSessionExpirationMs = 3600000; // 1 hour
    private final int maxFeedbackPerSession = 1000;

    // Builder pattern...
}
```

## Validation

### IFeedbackValidator

```java
public interface IFeedbackValidator {
    /**
     * Validate feedback before processing
     * @param feedback Feedback to validate
     * @return Validation result
     */
    ValidationResult validate(Feedback feedback);

    /**
     * Validate batch feedback
     * @param feedbackList List of feedback items
     * @return Batch validation result
     */
    BatchValidationResult validateBatch(List<Feedback> feedbackList);

    /**
     * Check if message can receive feedback
     * @param messageId Message ID
     * @return true if feedback allowed
     */
    boolean canReceiveFeedback(String messageId);

    /**
     * Check if user has feedback rate limit
     * @param userId User ID
     * @return Rate limit status
     */
    RateLimitStatus checkRateLimit(String userId);
}
```

### ValidationResult

```java
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = Collections.unmodifiableList(errors);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }

    public static ValidationResult failure(String... errors) {
        return new ValidationResult(false, Arrays.asList(errors), Collections.emptyList());
    }

    // Getters...
}
```

## Error Handling

### FeedbackException

```java
public class FeedbackException extends Exception {
    public enum ErrorType {
        VALIDATION_ERROR,
        PROCESSING_ERROR,
        UNDO_ERROR,
        MODEL_UPDATE_ERROR,
        SESSION_ERROR,
        RATE_LIMIT_EXCEEDED
    }

    private final ErrorType errorType;
    private final Feedback feedback;

    public FeedbackException(ErrorType errorType, String message,
                            Feedback feedback, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.feedback = feedback;
    }

    // Getters...
}
```

## Slack Integration

### IFeedbackSlackAdapter

```java
public interface IFeedbackSlackAdapter {
    /**
     * Handle feedback slash command
     * @param command Slash command payload
     * @return Command response
     */
    CompletableFuture<SlashCommandResponse> handleFeedbackCommand(
        SlashCommandPayload command
    );

    /**
     * Handle feedback button interaction
     * @param interaction Button interaction payload
     * @return Interaction response
     */
    CompletableFuture<InteractionResponse> handleFeedbackButton(
        InteractionPayload interaction
    );

    /**
     * Send feedback confirmation
     * @param userId User ID
     * @param result Feedback result
     */
    void sendFeedbackConfirmation(String userId, FeedbackResult result);

    /**
     * Send undo confirmation
     * @param userId User ID
     * @param result Undo result
     */
    void sendUndoConfirmation(String userId, UndoResult result);

    /**
     * Generate feedback UI blocks
     * @param message Message to create feedback for
     * @return Slack blocks for feedback UI
     */
    List<Block> generateFeedbackBlocks(Message message);
}
```

## Performance Requirements

### Latency
- Single feedback processing: < 500ms
- Batch feedback processing: < 2000ms
- Undo operation: < 300ms
- Feedback history retrieval: < 100ms

### Throughput
- Feedback processing: 100 items/second
- Concurrent users: 50
- Batch sessions: 10 concurrent

### Resource Limits
- Undo history per user: 100 items
- Batch session size: 1000 items maximum
- Session expiration: 1 hour

## Thread Safety

All implementations must be thread-safe for:
- Concurrent feedback processing
- Simultaneous batch sessions
- Undo operations during processing
- Model updates during feedback collection

## Testing Requirements

### Unit Tests
- Test feedback processing logic
- Test undo functionality
- Test batch operations
- Test validation rules

### Integration Tests
- Test end-to-end feedback flow
- Test Slack command integration
- Test model update triggering
- Test concurrent operations

### Performance Tests
- Load test with 100 concurrent feedback
- Test batch processing performance
- Test undo under load

## Security Requirements

### Input Validation
- Validate message IDs
- Validate user IDs
- Validate feedback type
- Rate limiting per user

### Data Protection
- Don't log message content
- Secure undo tokens
- Audit feedback trail
- Clean up old data

## Usage Examples

### Single Feedback

```java
IFeedbackProcessor processor = // injection
Feedback feedback = new Feedback(
    messageId: "msg_123",
    userId: "user_456",
    type: FeedbackType.TOO_LOW,
    originalScore: currentScore
);

processor.processFeedback(feedback)
    .thenAccept(result -> {
        if (result.isSuccess()) {
            System.out.println("Feedback processed: " + result.getUndoToken());
        }
    });
```

### Batch Feedback

```java
IBatchFeedbackManager batchManager = // injection
String sessionId = batchManager.startBatchSession(userId);

for (Feedback feedback : feedbackItems) {
    batchManager.addToBatch(sessionId, feedback);
}

batchManager.submitBatch(sessionId)
    .thenAccept(result -> {
        System.out.println("Processed " + result.getSuccessful() +
                          " out of " + result.getTotalFeedback());
    });
```

### Undo Feedback

```java
IUndoManager undoManager = // injection
undoManager.undoByToken(undoToken)
    .thenAccept(result -> {
        if (result.isSuccess()) {
            System.out.println("Feedback undone successfully");
        }
    });
```