# SlackGrab Integration Points Specification

## Overview

This document provides detailed specifications for all integration points in SlackGrab. Each integration point is fully specified with request/response formats, error handling, security requirements, and implementation details.

## 1. Slack API Integration

### 1.1 OAuth 2.0 Authentication Flow

#### Authorization Request
```http
GET https://slack.com/oauth/v2/authorize
Parameters:
  client_id: {SLACK_CLIENT_ID}
  scope: chat:write,channels:history,channels:read,groups:history,groups:read,im:history,mpim:history,users:read,team:read
  redirect_uri: http://localhost:7395/oauth/callback
  state: {CSRF_TOKEN}
  code_challenge: {PKCE_CHALLENGE}
  code_challenge_method: S256
```

#### Token Exchange
```http
POST https://slack.com/api/oauth.v2.access
Content-Type: application/x-www-form-urlencoded

client_id={CLIENT_ID}
&client_secret={CLIENT_SECRET}
&code={AUTHORIZATION_CODE}
&redirect_uri=http://localhost:7395/oauth/callback
&code_verifier={PKCE_VERIFIER}
```

**Response:**
```json
{
  "ok": true,
  "access_token": "xoxb-...",
  "token_type": "bot",
  "scope": "chat:write,channels:history,...",
  "bot_user_id": "U12345678",
  "app_id": "A12345678",
  "team": {
    "id": "T12345678",
    "name": "Workspace Name"
  },
  "authed_user": {
    "id": "U87654321",
    "scope": "identify,channels:read,...",
    "access_token": "xoxp-...",
    "token_type": "user"
  }
}
```

### 1.2 Message Fetching

#### Conversations List
```http
GET https://slack.com/api/conversations.list
Headers:
  Authorization: Bearer {BOT_TOKEN}
Parameters:
  types: public_channel,private_channel,mpim,im
  limit: 200
  cursor: {PAGINATION_CURSOR}
```

#### Conversation History
```http
GET https://slack.com/api/conversations.history
Headers:
  Authorization: Bearer {BOT_TOKEN}
Parameters:
  channel: {CHANNEL_ID}
  oldest: {TIMESTAMP_30_DAYS_AGO}
  limit: 100
  cursor: {PAGINATION_CURSOR}
```

**Response Structure:**
```json
{
  "ok": true,
  "messages": [
    {
      "type": "message",
      "user": "U12345678",
      "text": "Message content",
      "ts": "1234567890.123456",
      "channel": "C12345678",
      "attachments": [],
      "files": [],
      "reactions": [
        {
          "name": "thumbsup",
          "users": ["U87654321"],
          "count": 1
        }
      ],
      "thread_ts": "1234567890.123456",
      "reply_count": 3
    }
  ],
  "has_more": true,
  "response_metadata": {
    "next_cursor": "bmV4dF90czoxNTEyMDg1ODYxMDAwNTQz"
  }
}
```

### 1.3 Rate Limiting

**Rate Limit Headers:**
```http
X-Rate-Limit-Limit: 50
X-Rate-Limit-Remaining: 45
X-Rate-Limit-Reset: 1234567890
Retry-After: 30
```

**Rate Limit Response:**
```json
{
  "ok": false,
  "error": "rate_limited",
  "retry_after": 30
}
```

**Implementation:**
```java
public class RateLimitManager {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public CompletableFuture<Response> execute(String method, Request request) {
        Bucket bucket = buckets.computeIfAbsent(method, k -> new Bucket(50, 60000));

        if (!bucket.tryConsume(1)) {
            long waitTime = bucket.getWaitTime();
            return CompletableFuture.supplyAsync(() -> execute(method, request),
                delayedExecutor(waitTime, TimeUnit.MILLISECONDS));
        }

        return sendRequest(request).thenApply(response -> {
            updateBucket(bucket, response);
            return response;
        });
    }
}
```

## 2. Slack Apps Platform Integration

### 2.1 App Home Tab

#### Home Tab View
```json
{
  "type": "home",
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "*High Priority Messages*"
      }
    },
    {
      "type": "divider"
    },
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "<https://slack.com/archives/C1234/p1234567890|Important message from @boss>"
      },
      "accessory": {
        "type": "button",
        "text": {
          "type": "plain_text",
          "text": "Mark as Read"
        },
        "action_id": "mark_read",
        "value": "msg_123456"
      }
    }
  ]
}
```

#### Update Home Tab
```http
POST https://slack.com/api/views.publish
Headers:
  Authorization: Bearer {BOT_TOKEN}
  Content-Type: application/json

{
  "user_id": "U12345678",
  "view": {
    "type": "home",
    "blocks": [...]
  }
}
```

### 2.2 Slash Commands

#### Command Registration (App Manifest)
```yaml
slash_commands:
  - command: /slackgrab
    url: http://localhost:7395/slack/commands
    description: SlackGrab commands
    usage_hint: "[feedback|status|help]"
    should_escape: false
```

#### Command Payload
```json
{
  "token": "verification_token",
  "team_id": "T12345678",
  "team_domain": "workspace",
  "channel_id": "C12345678",
  "channel_name": "general",
  "user_id": "U12345678",
  "user_name": "username",
  "command": "/slackgrab",
  "text": "feedback too-high msg_123456",
  "response_url": "https://hooks.slack.com/commands/...",
  "trigger_id": "123456.789012.abcdef"
}
```

#### Command Response
```json
{
  "response_type": "ephemeral",
  "text": "Feedback recorded. The model will learn from this."
}
```

### 2.3 Interactive Components

#### Button Click Payload
```json
{
  "type": "block_actions",
  "user": {
    "id": "U12345678",
    "username": "username"
  },
  "api_app_id": "A12345678",
  "token": "verification_token",
  "trigger_id": "123456.789012.abcdef",
  "team": {
    "id": "T12345678",
    "domain": "workspace"
  },
  "channel": {
    "id": "C12345678",
    "name": "general"
  },
  "actions": [
    {
      "type": "button",
      "action_id": "feedback_too_low",
      "block_id": "msg_123456",
      "value": "too_low",
      "action_ts": "1234567890.123456"
    }
  ]
}
```

### 2.4 Bot Messages

#### Post Message
```http
POST https://slack.com/api/chat.postMessage
Headers:
  Authorization: Bearer {BOT_TOKEN}
  Content-Type: application/json

{
  "channel": "C12345678",
  "text": "High Priority Summary",
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "*📊 Hourly Priority Summary*\n\nYou have 5 high-priority messages:"
      }
    },
    {
      "type": "section",
      "fields": [
        {
          "type": "mrkdwn",
          "text": "*From:* @boss\n*Channel:* #important\n<https://slack.com/archives/C1234/p1234567890|View message>"
        }
      ]
    }
  ]
}
```

## 3. Webhook Event Handling

### 3.1 Event Subscription

#### URL Verification
```json
{
  "type": "url_verification",
  "token": "verification_token",
  "challenge": "3eZbrw1aBm2rZgRNFdxV2595E9CY3gmdALWMmHkvFXO7tYXAYM8P"
}
```

**Response:**
```json
{
  "challenge": "3eZbrw1aBm2rZgRNFdxV2595E9CY3gmdALWMmHkvFXO7tYXAYM8P"
}
```

### 3.2 Message Events

#### New Message Event
```json
{
  "token": "verification_token",
  "team_id": "T12345678",
  "api_app_id": "A12345678",
  "event": {
    "type": "message",
    "channel": "C12345678",
    "user": "U12345678",
    "text": "Important message content",
    "ts": "1234567890.123456",
    "event_ts": "1234567890.123456",
    "channel_type": "channel",
    "files": [
      {
        "id": "F12345678",
        "name": "document.pdf",
        "mimetype": "application/pdf",
        "size": 123456
      }
    ]
  },
  "type": "event_callback",
  "event_id": "Ev12345678",
  "event_time": 1234567890
}
```

#### Message Reaction Event
```json
{
  "token": "verification_token",
  "team_id": "T12345678",
  "api_app_id": "A12345678",
  "event": {
    "type": "reaction_added",
    "user": "U12345678",
    "item": {
      "type": "message",
      "channel": "C12345678",
      "ts": "1234567890.123456"
    },
    "reaction": "thumbsup",
    "event_ts": "1234567891.123456"
  },
  "type": "event_callback",
  "event_id": "Ev12345679",
  "event_time": 1234567891
}
```

### 3.3 Webhook Security

#### Request Signature Verification
```java
public boolean verifySlackSignature(HttpServletRequest request) {
    String timestamp = request.getHeader("X-Slack-Request-Timestamp");
    String signature = request.getHeader("X-Slack-Signature");
    String body = readRequestBody(request);

    // Verify timestamp is within 5 minutes
    long requestTime = Long.parseLong(timestamp);
    long currentTime = System.currentTimeMillis() / 1000;
    if (Math.abs(currentTime - requestTime) > 300) {
        return false;
    }

    // Compute expected signature
    String baseString = "v0:" + timestamp + ":" + body;
    String expectedSignature = "v0=" + hmacSha256(SIGNING_SECRET, baseString);

    return MessageDigest.isEqual(
        signature.getBytes(StandardCharsets.UTF_8),
        expectedSignature.getBytes(StandardCharsets.UTF_8)
    );
}
```

## 4. GPU/CPU Resource Management

### 4.1 GPU Detection and Selection

#### Intel GPU Detection
```java
public class GPUDetector {
    public GPUInfo detectGPU() {
        // Check for Intel GPU via oneAPI
        if (IntelGPUDetector.isAvailable()) {
            return new GPUInfo(
                type: GPUType.INTEL,
                memory: IntelGPUDetector.getMemory(),
                driver: "Intel oneAPI",
                maxMemoryUsage: 0.8
            );
        }

        // Check for NVIDIA GPU via CUDA
        if (CudaDetector.isAvailable()) {
            return new GPUInfo(
                type: GPUType.NVIDIA,
                memory: CudaDetector.getMemory(),
                driver: "CUDA 12.0",
                maxMemoryUsage: 0.8
            );
        }

        // Fallback to CPU
        return new GPUInfo(
            type: GPUType.CPU_ONLY,
            memory: Runtime.getRuntime().maxMemory(),
            driver: "CPU",
            maxMemoryUsage: 1.0
        );
    }
}
```

### 4.2 Resource Monitoring

#### System Resource Check
```java
public class ResourceMonitor {
    private static final double CPU_THRESHOLD = 0.80;
    private static final double GPU_MEMORY_THRESHOLD = 0.80;

    public ResourceStatus checkResources() {
        double cpuUsage = getCurrentCPUUsage();
        double gpuMemoryUsage = getCurrentGPUMemoryUsage();

        return new ResourceStatus(
            cpuAvailable: cpuUsage < CPU_THRESHOLD,
            gpuAvailable: gpuMemoryUsage < GPU_MEMORY_THRESHOLD,
            cpuUsage: cpuUsage,
            gpuMemoryUsage: gpuMemoryUsage,
            shouldPauseTraining: cpuUsage > CPU_THRESHOLD
        );
    }

    private double getCurrentCPUUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
        }
        return 0.0;
    }
}
```

### 4.3 Dynamic Resource Allocation

```java
public class ResourceAllocator {
    public ExecutionStrategy selectStrategy(ResourceStatus status, TaskType task) {
        if (task == TaskType.TRAINING) {
            if (status.cpuUsage > 0.80) {
                return new PausedStrategy();
            }
            if (status.gpuAvailable && status.gpuMemoryUsage < 0.80) {
                return new GPUStrategy(batchSize: 256);
            }
            return new CPUStrategy(batchSize: 32);
        }

        if (task == TaskType.INFERENCE) {
            if (status.gpuAvailable) {
                return new GPUStrategy(batchSize: 1);
            }
            return new CPUStrategy(batchSize: 1);
        }

        return new DefaultStrategy();
    }
}
```

## 5. Windows Credential Storage Integration

### 5.1 Credential Manager API

#### Store Credentials
```java
public class WindowsCredentialManager {
    static {
        Native.register("Advapi32");
    }

    public void storeCredential(String target, String username, String password) {
        CREDENTIAL credential = new CREDENTIAL();
        credential.Type = CRED_TYPE_GENERIC;
        credential.TargetName = target;
        credential.UserName = username;
        credential.CredentialBlobSize = password.length() * 2;
        credential.CredentialBlob = password.getBytes(StandardCharsets.UTF_16LE);
        credential.Persist = CRED_PERSIST_LOCAL_MACHINE;

        if (!CredWrite(credential, 0)) {
            throw new SecurityException("Failed to store credential");
        }
    }

    public String retrieveCredential(String target) {
        CREDENTIAL.ByReference pcred = new CREDENTIAL.ByReference();

        if (!CredRead(target, CRED_TYPE_GENERIC, 0, pcred)) {
            return null;
        }

        try {
            byte[] passwordBytes = pcred.CredentialBlob.getByteArray(0, pcred.CredentialBlobSize);
            return new String(passwordBytes, StandardCharsets.UTF_16LE);
        } finally {
            CredFree(pcred);
        }
    }
}
```

### 5.2 Token Storage Schema

```java
public class TokenStorage {
    private static final String TARGET_PREFIX = "SlackGrab:";

    public void storeTokens(OAuthTokens tokens) {
        credentialManager.storeCredential(
            TARGET_PREFIX + "bot_token",
            tokens.getBotUserId(),
            tokens.getBotAccessToken()
        );

        credentialManager.storeCredential(
            TARGET_PREFIX + "user_token",
            tokens.getUserId(),
            tokens.getUserAccessToken()
        );

        credentialManager.storeCredential(
            TARGET_PREFIX + "refresh_token",
            tokens.getUserId(),
            tokens.getRefreshToken()
        );
    }
}
```

## 6. Local File System Integration

### 6.1 Directory Structure

```java
public class FileSystemLayout {
    private static final Path BASE_DIR = Paths.get(
        System.getenv("LOCALAPPDATA"), "SlackGrab"
    );

    public static final Path DATABASE_DIR = BASE_DIR.resolve("database");
    public static final Path LOGS_DIR = BASE_DIR.resolve("logs");
    public static final Path CACHE_DIR = BASE_DIR.resolve("cache");
    public static final Path MODELS_DIR = BASE_DIR.resolve("models");
    public static final Path CONFIG_DIR = BASE_DIR.resolve("config");

    public void initializeDirectories() {
        List<Path> directories = List.of(
            DATABASE_DIR, LOGS_DIR, CACHE_DIR, MODELS_DIR, CONFIG_DIR
        );

        for (Path dir : directories) {
            try {
                Files.createDirectories(dir);
                // Set appropriate permissions
                if (SystemUtils.IS_OS_WINDOWS) {
                    Files.setAttribute(dir, "dos:hidden", false);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory: " + dir, e);
            }
        }
    }
}
```

### 6.2 Database File Management

```java
public class DatabaseFileManager {
    private static final String DB_FILENAME = "slackgrab.db";
    private static final String BACKUP_PREFIX = "slackgrab_backup_";

    public Path getDatabasePath() {
        return FileSystemLayout.DATABASE_DIR.resolve(DB_FILENAME);
    }

    public void createBackup() {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );
        Path backupPath = FileSystemLayout.DATABASE_DIR.resolve(
            BACKUP_PREFIX + timestamp + ".db"
        );

        Files.copy(getDatabasePath(), backupPath,
            StandardCopyOption.COPY_ATTRIBUTES);

        // Keep only last 5 backups
        cleanOldBackups(5);
    }
}
```

### 6.3 Log Rotation

```xml
<!-- logback.xml configuration -->
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOCALAPPDATA}/SlackGrab/logs/slackgrab.log</file>
        <encoder>
            <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOCALAPPDATA}/SlackGrab/logs/slackgrab.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>100MB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

## 7. Neural Network Integration

### 7.1 Model Loading and Initialization

```java
public class NeuralNetworkLoader {
    private static final Path MODEL_PATH = FileSystemLayout.MODELS_DIR.resolve("importance_model.zip");

    public MultiLayerNetwork loadModel() {
        if (!Files.exists(MODEL_PATH)) {
            return createDefaultModel();
        }

        try {
            ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH.toFile());
        } catch (IOException e) {
            log.error("Failed to load model, creating new", e);
            return createDefaultModel();
        }
    }

    private MultiLayerNetwork createDefaultModel() {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
            .seed(42)
            .updater(new Adam(0.001))
            .list()
            .layer(new DenseLayer.Builder()
                .nIn(768)  // BERT embedding size
                .nOut(512)
                .activation(Activation.RELU)
                .build())
            .layer(new DenseLayer.Builder()
                .nIn(512)
                .nOut(256)
                .activation(Activation.RELU)
                .dropOut(0.5)
                .build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                .nIn(256)
                .nOut(3)  // High, Medium, Low
                .activation(Activation.SOFTMAX)
                .build())
            .build();

        return new MultiLayerNetwork(config);
    }
}
```

### 7.2 Feature Extraction

```java
public class MessageFeatureExtractor {
    private final SentenceTransformer embedder;

    public INDArray extractFeatures(Message message, Context context) {
        // Text embedding (768 dims)
        float[] textEmbedding = embedder.encode(message.getText());

        // Additional features
        float[] features = new float[] {
            // Sender features (10 dims)
            context.getSenderImportance(message.getUserId()),
            context.getSenderResponseRate(message.getUserId()),
            context.getSenderInteractionScore(message.getUserId()),

            // Channel features (10 dims)
            context.getChannelImportance(message.getChannelId()),
            context.getChannelActivityLevel(message.getChannelId()),

            // Temporal features (10 dims)
            getHourOfDay(message.getTimestamp()),
            getDayOfWeek(message.getTimestamp()),
            getTimeSinceLastMessage(message),

            // Content features (10 dims)
            message.hasAttachments() ? 1.0f : 0.0f,
            message.hasLinks() ? 1.0f : 0.0f,
            message.hasMentions() ? 1.0f : 0.0f,
            message.isThreadReply() ? 1.0f : 0.0f,
            message.getReactionCount() / 10.0f,

            // Urgency indicators (10 dims)
            containsUrgentKeywords(message.getText()) ? 1.0f : 0.0f,
            message.getText().length() / 1000.0f
        };

        return Nd4j.concat(1,
            Nd4j.create(textEmbedding),
            Nd4j.create(features)
        );
    }
}
```

### 7.3 Incremental Training

```java
public class IncrementalTrainer {
    private final ExperienceReplayBuffer buffer = new ExperienceReplayBuffer(10000);

    public void train(Feedback feedback) {
        // Add to replay buffer
        TrainingSample sample = createSample(feedback);
        buffer.add(sample);

        // Check if training should occur
        if (buffer.size() >= BATCH_SIZE && resourceMonitor.canTrain()) {
            List<TrainingSample> batch = buffer.sample(BATCH_SIZE);

            INDArray features = Nd4j.create(batch.size(), FEATURE_SIZE);
            INDArray labels = Nd4j.create(batch.size(), 3);

            for (int i = 0; i < batch.size(); i++) {
                features.putRow(i, batch.get(i).getFeatures());
                labels.putRow(i, batch.get(i).getLabel());
            }

            DataSet dataSet = new DataSet(features, labels);
            model.fit(dataSet);

            // Save checkpoint every 100 batches
            if (++batchCount % 100 == 0) {
                saveCheckpoint();
            }
        }
    }
}
```

## 8. Webhook Server Implementation

### 8.1 Server Configuration

```java
public class WebhookServer {
    private final Javalin app;
    private static final int PORT = 7395;

    public WebhookServer() {
        this.app = Javalin.create(config -> {
            config.server(() -> {
                Server server = new Server();
                ServerConnector connector = new ServerConnector(server);
                connector.setHost("127.0.0.1");  // Localhost only
                connector.setPort(PORT);
                server.setConnectors(new Connector[] { connector });
                return server;
            });
            config.showJavalinBanner = false;
            config.defaultContentType = "application/json";
            config.enableCorsForAllOrigins = false;  // Security
        });

        configureRoutes();
        configureErrorHandling();
    }

    private void configureRoutes() {
        // OAuth callback
        app.get("/oauth/callback", this::handleOAuthCallback);

        // Slack events
        app.post("/slack/events", this::handleSlackEvent);

        // Slash commands
        app.post("/slack/commands", this::handleSlashCommand);

        // Interactive components
        app.post("/slack/interactive", this::handleInteractive);

        // Health check
        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));
    }
}
```

### 8.2 Request Handling

```java
public class SlackEventHandler {
    public void handleSlackEvent(Context ctx) {
        // Verify signature
        if (!verifySlackSignature(ctx)) {
            ctx.status(401);
            return;
        }

        JsonNode payload = ctx.bodyAsClass(JsonNode.class);
        String type = payload.get("type").asText();

        switch (type) {
            case "url_verification":
                handleUrlVerification(ctx, payload);
                break;

            case "event_callback":
                handleEventCallback(ctx, payload);
                break;

            default:
                ctx.status(400);
        }
    }

    private void handleEventCallback(Context ctx, JsonNode payload) {
        // Acknowledge immediately
        ctx.status(200);

        // Process asynchronously
        eventProcessor.processAsync(payload.get("event"));
    }
}
```

## 9. Error Handling Integration

### 9.1 Global Error Handler

```java
public class GlobalErrorHandler {
    private final Logger errorLog = LoggerFactory.getLogger("ERROR");

    public void handleError(Throwable error, ErrorContext context) {
        ErrorCategory category = categorizeError(error);

        switch (category) {
            case CRITICAL:
                handleCriticalError(error, context);
                break;

            case RECOVERABLE:
                handleRecoverableError(error, context);
                break;

            case DEGRADED:
                handleDegradedMode(error, context);
                break;

            case WARNING:
                logWarning(error, context);
                break;
        }
    }

    private void handleRecoverableError(Throwable error, ErrorContext context) {
        errorLog.error("Recoverable error in {}: {}",
            context.getComponent(), error.getMessage());

        // Attempt recovery
        RecoveryStrategy strategy = selectRecoveryStrategy(error, context);
        strategy.recover();
    }
}
```

### 9.2 Circuit Breaker Implementation

```java
public class SlackApiCircuitBreaker {
    private final CircuitBreaker circuitBreaker;

    public SlackApiCircuitBreaker() {
        this.circuitBreaker = CircuitBreaker.ofDefaults("slack-api");
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Circuit breaker state transition: {}", event));
    }

    public <T> CompletableFuture<T> executeCall(Supplier<T> apiCall) {
        return CompletableFuture.supplyAsync(() ->
            circuitBreaker.executeSupplier(apiCall)
        ).exceptionally(throwable -> {
            if (throwable instanceof CallNotPermittedException) {
                // Circuit is open, use cached data
                return getCachedResponse();
            }
            throw new RuntimeException(throwable);
        });
    }
}
```

## 10. Security Integration

### 10.1 Encryption Service

```java
public class EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKey masterKey;

    public EncryptionService() {
        this.masterKey = loadOrGenerateMasterKey();
    }

    public byte[] encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = generateIV();

            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

            byte[] ciphertext = cipher.doFinal(
                plaintext.getBytes(StandardCharsets.UTF_8)
            );

            // Combine IV and ciphertext
            byte[] result = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);

            return result;
        } catch (Exception e) {
            throw new SecurityException("Encryption failed", e);
        }
    }
}
```

### 10.2 Input Validation

```java
public class InputValidator {
    private static final int MAX_MESSAGE_LENGTH = 40000;
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("^[CDG][A-Z0-9]{8,}$");

    public ValidationResult validateMessage(Message message) {
        List<String> errors = new ArrayList<>();

        if (message.getText().length() > MAX_MESSAGE_LENGTH) {
            errors.add("Message exceeds maximum length");
        }

        if (!CHANNEL_ID_PATTERN.matcher(message.getChannelId()).matches()) {
            errors.add("Invalid channel ID format");
        }

        if (message.getTimestamp() > System.currentTimeMillis() / 1000 + 60) {
            errors.add("Message timestamp is in the future");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
```

## Integration Testing Checklist

### Slack API Integration
- [ ] OAuth flow completes successfully
- [ ] Token refresh works before expiration
- [ ] Rate limiting handled gracefully
- [ ] All API endpoints return expected data
- [ ] Error responses handled correctly

### Webhook Server
- [ ] Server starts on localhost:7395
- [ ] Signature verification works
- [ ] Events processed asynchronously
- [ ] Commands respond within 3 seconds
- [ ] No external access possible

### Neural Network
- [ ] Model loads successfully
- [ ] GPU acceleration detected and used
- [ ] CPU fallback works
- [ ] Scoring completes in < 1 second
- [ ] Training pauses when CPU > 80%

### Windows Integration
- [ ] Credentials stored securely
- [ ] Auto-start registration works
- [ ] System tray icon appears
- [ ] File paths use correct separators

### Error Handling
- [ ] All errors logged silently
- [ ] No user popups appear
- [ ] Recovery mechanisms work
- [ ] Circuit breakers function correctly

## Conclusion

These integration specifications provide complete contracts for all external and internal system boundaries. Each integration point includes request/response formats, error handling, security requirements, and implementation details. Teams can work independently using these specifications, with confidence that components will integrate correctly when combined.