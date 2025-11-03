# Data Layer Interface Contract

## Overview
This contract defines all interfaces for data persistence, encryption, and repository patterns used throughout SlackGrab.

## Core Repository Interfaces

### IMessageRepository

```java
public interface IMessageRepository {
    /**
     * Save a message to the database
     * @param message Message to save
     * @return Saved message with generated ID
     */
    Message save(Message message);

    /**
     * Batch save messages
     * @param messages Messages to save
     * @return Number of messages saved
     */
    int batchSave(List<Message> messages);

    /**
     * Find message by ID
     * @param messageId Message ID
     * @return Message or null if not found
     */
    Optional<Message> findById(String messageId);

    /**
     * Find messages by channel
     * @param channelId Channel ID
     * @param limit Maximum messages to return
     * @param offset Offset for pagination
     * @return List of messages
     */
    List<Message> findByChannel(String channelId, int limit, int offset);

    /**
     * Find messages by user
     * @param userId User ID
     * @param limit Maximum messages to return
     * @param offset Offset for pagination
     * @return List of messages
     */
    List<Message> findByUser(String userId, int limit, int offset);

    /**
     * Find messages within time range
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of messages
     */
    List<Message> findByTimeRange(long startTime, long endTime);

    /**
     * Find recent messages
     * @param days Number of days to look back
     * @return List of recent messages
     */
    List<Message> findRecent(int days);

    /**
     * Update message
     * @param message Message with updated fields
     * @return true if update successful
     */
    boolean update(Message message);

    /**
     * Delete old messages
     * @param olderThanDays Delete messages older than this many days
     * @return Number of messages deleted
     */
    int deleteOlderThan(int olderThanDays);

    /**
     * Count total messages
     * @return Total message count
     */
    long count();

    /**
     * Count messages by channel
     * @param channelId Channel ID
     * @return Message count for channel
     */
    long countByChannel(String channelId);

    /**
     * Check if message exists
     * @param messageId Message ID
     * @return true if message exists
     */
    boolean exists(String messageId);
}
```

### ITrainingDataRepository

```java
public interface ITrainingDataRepository {
    /**
     * Save training sample
     * @param sample Training sample
     * @return Saved sample with generated ID
     */
    TrainingSample save(TrainingSample sample);

    /**
     * Batch save training samples
     * @param samples Samples to save
     * @return Number of samples saved
     */
    int batchSave(List<TrainingSample> samples);

    /**
     * Get training samples for batch
     * @param batchSize Number of samples to retrieve
     * @return List of training samples
     */
    List<TrainingSample> getSamplesForTraining(int batchSize);

    /**
     * Get samples with specific label
     * @param label Target label (HIGH, MEDIUM, LOW)
     * @param limit Maximum samples to return
     * @return List of training samples
     */
    List<TrainingSample> getSamplesByLabel(ImportanceLevel label, int limit);

    /**
     * Get validation dataset
     * @param percentage Percentage of data for validation (e.g., 0.2 for 20%)
     * @return Validation dataset
     */
    List<TrainingSample> getValidationSet(double percentage);

    /**
     * Mark samples as used in training
     * @param sampleIds List of sample IDs
     */
    void markAsUsed(List<Long> sampleIds);

    /**
     * Delete old training samples
     * @param olderThanDays Delete samples older than this many days
     * @return Number of samples deleted
     */
    int deleteOlderThan(int olderThanDays);

    /**
     * Get sample statistics
     * @return Training data statistics
     */
    TrainingDataStats getStatistics();

    /**
     * Count total samples
     * @return Total sample count
     */
    long count();

    /**
     * Count samples by label
     * @param label Importance level
     * @return Sample count for label
     */
    long countByLabel(ImportanceLevel label);
}
```

### IInteractionRepository

```java
public interface IInteractionRepository {
    /**
     * Record user interaction
     * @param interaction User interaction data
     * @return Saved interaction with generated ID
     */
    UserInteraction save(UserInteraction interaction);

    /**
     * Find interactions by message
     * @param messageId Message ID
     * @return List of interactions for message
     */
    List<UserInteraction> findByMessage(String messageId);

    /**
     * Find interactions by user
     * @param userId User ID
     * @param limit Maximum interactions to return
     * @return List of user interactions
     */
    List<UserInteraction> findByUser(String userId, int limit);

    /**
     * Find interactions within time range
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of interactions
     */
    List<UserInteraction> findByTimeRange(long startTime, long endTime);

    /**
     * Get interaction statistics for sender
     * @param senderId Sender ID
     * @return Sender interaction statistics
     */
    SenderStats getSenderStats(String senderId);

    /**
     * Get interaction statistics for channel
     * @param channelId Channel ID
     * @return Channel interaction statistics
     */
    ChannelStats getChannelStats(String channelId);

    /**
     * Get user behavior profile
     * @param userId User ID
     * @return User behavior profile
     */
    UserBehaviorProfile getUserProfile(String userId);

    /**
     * Delete old interactions
     * @param olderThanDays Delete interactions older than this many days
     * @return Number of interactions deleted
     */
    int deleteOlderThan(int olderThanDays);

    /**
     * Count total interactions
     * @return Total interaction count
     */
    long count();
}
```

### IModelCheckpointRepository

```java
public interface IModelCheckpointRepository {
    /**
     * Save model checkpoint
     * @param checkpoint Model checkpoint
     * @return Saved checkpoint with generated ID
     */
    ModelCheckpoint save(ModelCheckpoint checkpoint);

    /**
     * Find checkpoint by version
     * @param version Model version
     * @return Model checkpoint or null if not found
     */
    Optional<ModelCheckpoint> findByVersion(String version);

    /**
     * Find latest checkpoint
     * @return Latest model checkpoint
     */
    Optional<ModelCheckpoint> findLatest();

    /**
     * List all checkpoints
     * @return List of all checkpoints ordered by timestamp
     */
    List<ModelCheckpoint> listAll();

    /**
     * Delete checkpoint
     * @param checkpointId Checkpoint ID
     * @return true if deletion successful
     */
    boolean delete(long checkpointId);

    /**
     * Delete old checkpoints keeping specified number
     * @param keepCount Number of recent checkpoints to keep
     * @return Number of checkpoints deleted
     */
    int deleteOldCheckpoints(int keepCount);

    /**
     * Get checkpoint metadata
     * @param version Model version
     * @return Checkpoint metadata
     */
    Optional<CheckpointMetadata> getMetadata(String version);

    /**
     * Count total checkpoints
     * @return Total checkpoint count
     */
    long count();
}
```

### IMetricsRepository

```java
public interface IMetricsRepository {
    /**
     * Save performance metric
     * @param metric Performance metric
     */
    void save(PerformanceMetric metric);

    /**
     * Batch save metrics
     * @param metrics List of metrics
     */
    void batchSave(List<PerformanceMetric> metrics);

    /**
     * Get metrics by type
     * @param type Metric type
     * @param limit Maximum metrics to return
     * @return List of metrics
     */
    List<PerformanceMetric> findByType(MetricType type, int limit);

    /**
     * Get metrics within time range
     * @param type Metric type
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of metrics
     */
    List<PerformanceMetric> findByTimeRange(
        MetricType type,
        long startTime,
        long endTime
    );

    /**
     * Get aggregated metrics
     * @param type Metric type
     * @param period Aggregation period (HOURLY, DAILY)
     * @param days Number of days to aggregate
     * @return Aggregated metrics
     */
    List<AggregatedMetric> getAggregated(
        MetricType type,
        AggregationPeriod period,
        int days
    );

    /**
     * Delete old metrics
     * @param olderThanDays Delete metrics older than this many days
     * @return Number of metrics deleted
     */
    int deleteOlderThan(int olderThanDays);

    /**
     * Count metrics by type
     * @param type Metric type
     * @return Metric count
     */
    long countByType(MetricType type);
}
```

## Encryption Interfaces

### IEncryptionService

```java
public interface IEncryptionService {
    /**
     * Encrypt sensitive data
     * @param plaintext Data to encrypt
     * @return Encrypted data
     */
    byte[] encrypt(String plaintext);

    /**
     * Decrypt sensitive data
     * @param ciphertext Encrypted data
     * @return Decrypted data
     */
    String decrypt(byte[] ciphertext);

    /**
     * Encrypt field value
     * @param value Field value to encrypt
     * @param fieldName Field name for key derivation
     * @return Encrypted value
     */
    String encryptField(String value, String fieldName);

    /**
     * Decrypt field value
     * @param encryptedValue Encrypted field value
     * @param fieldName Field name for key derivation
     * @return Decrypted value
     */
    String decryptField(String encryptedValue, String fieldName);

    /**
     * Generate hash for value
     * @param value Value to hash
     * @return Hash value
     */
    String hash(String value);

    /**
     * Verify hash
     * @param value Original value
     * @param hash Hash to verify
     * @return true if hash matches
     */
    boolean verifyHash(String value, String hash);

    /**
     * Rotate encryption keys
     * @return true if rotation successful
     */
    boolean rotateKeys();
}
```

## Transaction Management

### ITransactionManager

```java
public interface ITransactionManager {
    /**
     * Begin a transaction
     * @return Transaction object
     */
    Transaction begin();

    /**
     * Execute operation within transaction
     * @param operation Operation to execute
     * @param <T> Return type
     * @return Operation result
     */
    <T> T executeInTransaction(TransactionalOperation<T> operation);

    /**
     * Execute operation with retry
     * @param operation Operation to execute
     * @param maxRetries Maximum retry attempts
     * @param <T> Return type
     * @return Operation result
     */
    <T> T executeWithRetry(
        TransactionalOperation<T> operation,
        int maxRetries
    );
}
```

### Transaction

```java
public interface Transaction extends AutoCloseable {
    /**
     * Commit the transaction
     */
    void commit();

    /**
     * Rollback the transaction
     */
    void rollback();

    /**
     * Check if transaction is active
     * @return true if active
     */
    boolean isActive();

    /**
     * Set savepoint
     * @param name Savepoint name
     */
    void setSavepoint(String name);

    /**
     * Rollback to savepoint
     * @param name Savepoint name
     */
    void rollbackToSavepoint(String name);

    @Override
    void close();
}
```

## Data Transfer Objects

### Message

```java
public class Message {
    private String id;
    private String channelId;
    private String userId;
    private String text;
    private long timestamp;
    private String threadTs;
    private boolean hasAttachments;
    private boolean hasLinks;
    private boolean hasMentions;
    private int reactionCount;
    private ImportanceLevel scoredLevel;
    private double scoredConfidence;
    private long scoredAt;
    private byte[] encryptedContent;

    // Constructor, getters, setters...

    public static class Builder {
        // Builder implementation...
    }
}
```

### TrainingSample

```java
public class TrainingSample {
    private Long id;
    private String messageId;
    private float[] features;
    private ImportanceLevel label;
    private FeedbackType feedbackType;
    private long timestamp;
    private boolean used;
    private int useCount;

    // Constructor, getters, setters...
}
```

### UserInteraction

```java
public class UserInteraction {
    public enum InteractionType {
        READ,
        REPLY,
        REACTION,
        THREAD_VIEW,
        CHANNEL_SWITCH,
        MESSAGE_DELETE
    }

    private Long id;
    private String messageId;
    private String userId;
    private InteractionType type;
    private long timestamp;
    private long durationMs;
    private Map<String, String> metadata;

    // Constructor, getters, setters...
}
```

### ModelCheckpoint

```java
public class ModelCheckpoint {
    private Long id;
    private String version;
    private String modelPath;
    private long timestamp;
    private double accuracy;
    private double loss;
    private Map<String, Double> metrics;
    private long sizeBytes;
    private String checksum;

    // Constructor, getters, setters...
}
```

### PerformanceMetric

```java
public class PerformanceMetric {
    private Long id;
    private MetricType type;
    private double value;
    private long timestamp;
    private String component;
    private Map<String, String> tags;

    // Constructor, getters, setters...
}
```

## Statistics Objects

### SenderStats

```java
public class SenderStats {
    private final String senderId;
    private final int messageCount;
    private final int readCount;
    private final int replyCount;
    private final int reactionCount;
    private final double averageReadTimeMs;
    private final double responseRatePercentage;
    private final double importanceScore;

    // Constructor, getters...
}
```

### ChannelStats

```java
public class ChannelStats {
    private final String channelId;
    private final int messageCount;
    private final int activeUsers;
    private final double averageResponseTimeMs;
    private final double engagementRate;
    private final Map<String, Integer> topSenders;

    // Constructor, getters...
}
```

### TrainingDataStats

```java
public class TrainingDataStats {
    private final long totalSamples;
    private final Map<ImportanceLevel, Long> samplesPerLabel;
    private final long unusedSamples;
    private final double labelBalanceScore;
    private final long oldestSampleAge;

    // Constructor, getters...

    public boolean isBalanced() {
        // Check if labels are reasonably balanced
        long min = samplesPerLabel.values().stream().min(Long::compareTo).orElse(0L);
        long max = samplesPerLabel.values().stream().max(Long::compareTo).orElse(0L);
        return max == 0 || (double) min / max > 0.5;
    }
}
```

## Enumerations

### MetricType

```java
public enum MetricType {
    INFERENCE_TIME,
    TRAINING_TIME,
    API_CALL_LATENCY,
    MEMORY_USAGE,
    CPU_USAGE,
    GPU_USAGE,
    ACCURACY,
    CACHE_HIT_RATE,
    ERROR_RATE
}
```

### AggregationPeriod

```java
public enum AggregationPeriod {
    HOURLY,
    DAILY,
    WEEKLY
}
```

## Database Schema Management

### ISchemaManager

```java
public interface ISchemaManager {
    /**
     * Initialize database schema
     * @return true if initialization successful
     */
    boolean initializeSchema();

    /**
     * Check if schema is up to date
     * @return true if schema is current version
     */
    boolean isSchemaUpToDate();

    /**
     * Migrate schema to latest version
     * @return Migration result
     */
    MigrationResult migrateSchema();

    /**
     * Get current schema version
     * @return Schema version
     */
    int getCurrentVersion();

    /**
     * Backup database
     * @param backupPath Backup file path
     * @return true if backup successful
     */
    boolean backup(Path backupPath);

    /**
     * Restore database from backup
     * @param backupPath Backup file path
     * @return true if restore successful
     */
    boolean restore(Path backupPath);
}
```

## Connection Management

### IDatabaseConnectionPool

```java
public interface IDatabaseConnectionPool {
    /**
     * Get connection from pool
     * @return Database connection
     */
    Connection getConnection() throws SQLException;

    /**
     * Return connection to pool
     * @param connection Connection to return
     */
    void returnConnection(Connection connection);

    /**
     * Get pool statistics
     * @return Pool statistics
     */
    PoolStatistics getStatistics();

    /**
     * Close all connections and shutdown pool
     */
    void shutdown();

    /**
     * Check pool health
     * @return true if pool is healthy
     */
    boolean isHealthy();
}
```

### PoolStatistics

```java
public class PoolStatistics {
    private final int activeConnections;
    private final int idleConnections;
    private final int totalConnections;
    private final long averageWaitTimeMs;
    private final int waitingThreads;

    // Constructor, getters...
}
```

## Error Handling

### DataAccessException

```java
public class DataAccessException extends RuntimeException {
    public enum ErrorType {
        CONNECTION_ERROR,
        QUERY_ERROR,
        CONSTRAINT_VIOLATION,
        ENCRYPTION_ERROR,
        TRANSACTION_ERROR,
        SCHEMA_ERROR
    }

    private final ErrorType errorType;
    private final String sqlState;

    public DataAccessException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.sqlState = extractSqlState(cause);
    }

    public boolean isRecoverable() {
        return errorType == ErrorType.CONNECTION_ERROR ||
               errorType == ErrorType.TRANSACTION_ERROR;
    }

    // Getters...
}
```

## Query Specifications

### IQueryBuilder

```java
public interface IQueryBuilder<T> {
    IQueryBuilder<T> where(String field, Object value);
    IQueryBuilder<T> and(String field, Object value);
    IQueryBuilder<T> or(String field, Object value);
    IQueryBuilder<T> orderBy(String field, boolean ascending);
    IQueryBuilder<T> limit(int limit);
    IQueryBuilder<T> offset(int offset);
    List<T> execute();
    Optional<T> executeSingle();
}
```

## Performance Requirements

### Latency Targets
- Single record insert: < 10ms
- Batch insert (100 records): < 100ms
- Simple query: < 50ms
- Complex query with joins: < 200ms
- Transaction commit: < 20ms

### Throughput Targets
- Inserts: 1000 records/second
- Queries: 5000 queries/second
- Concurrent connections: 50

### Resource Limits
- Database file size: < 2GB
- Memory usage: < 100MB
- Connection pool: 10-50 connections

## Thread Safety

All repository implementations must be thread-safe for:
- Concurrent read operations
- Concurrent write operations to different tables
- Transaction isolation
- Connection pool access

## Testing Requirements

### Unit Tests
- Test each repository method
- Test transaction management
- Test encryption/decryption
- Test error handling

### Integration Tests
- Test full CRUD operations
- Test concurrent access
- Test transaction rollback
- Test schema migration

### Performance Tests
- Bulk insert performance
- Query performance under load
- Connection pool behavior
- Memory usage patterns

## Security Requirements

### Data Protection
- Encrypt sensitive fields
- Hash user identifiers
- Secure key storage
- Audit sensitive operations

### Access Control
- Repository-level permissions
- Query parameter validation
- SQL injection prevention
- Path traversal prevention