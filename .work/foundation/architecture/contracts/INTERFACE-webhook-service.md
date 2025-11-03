# Webhook Service Interface Contract

## Overview
This contract defines all interfaces for the localhost webhook server that handles Slack events, commands, and interactive components.

## Core Interfaces

### IWebhookServer

```java
public interface IWebhookServer {
    /**
     * Start the webhook server
     * @param config Server configuration
     * @return true if started successfully
     */
    boolean start(WebhookServerConfig config);

    /**
     * Stop the webhook server gracefully
     * @param timeoutMs Maximum time to wait for shutdown
     * @return true if stopped cleanly
     */
    boolean stop(long timeoutMs);

    /**
     * Register route for Slack events
     * @param path URL path
     * @param handler Event handler
     */
    void registerEventRoute(String path, IEventRouteHandler handler);

    /**
     * Register route for slash commands
     * @param path URL path
     * @param handler Command handler
     */
    void registerCommandRoute(String path, ICommandRouteHandler handler);

    /**
     * Register route for interactive components
     * @param path URL path
     * @param handler Interaction handler
     */
    void registerInteractionRoute(String path, IInteractionRouteHandler handler);

    /**
     * Register OAuth callback route
     * @param path URL path
     * @param handler OAuth handler
     */
    void registerOAuthRoute(String path, IOAuthRouteHandler handler);

    /**
     * Get server status
     * @return Current server status
     */
    WebhookServerStatus getStatus();

    /**
     * Get server metrics
     * @return Server performance metrics
     */
    WebhookServerMetrics getMetrics();

    /**
     * Check if server is running
     * @return true if running
     */
    boolean isRunning();
}
```

### IEventRouteHandler

```java
public interface IEventRouteHandler {
    /**
     * Handle incoming Slack event
     * @param request HTTP request
     * @return HTTP response
     */
    HttpResponse handleEvent(HttpRequest request);

    /**
     * Validate event request
     * @param request HTTP request
     * @return Validation result
     */
    RequestValidationResult validate(HttpRequest request);

    /**
     * Get route path
     * @return Path this handler responds to
     */
    String getPath();

    /**
     * Get accepted HTTP methods
     * @return Set of HTTP methods
     */
    Set<HttpMethod> getAcceptedMethods();
}
```

### ICommandRouteHandler

```java
public interface ICommandRouteHandler {
    /**
     * Handle slash command request
     * @param request HTTP request
     * @return HTTP response (must be < 3 seconds)
     */
    HttpResponse handleCommand(HttpRequest request);

    /**
     * Validate command request
     * @param request HTTP request
     * @return Validation result
     */
    RequestValidationResult validate(HttpRequest request);

    /**
     * Parse command payload
     * @param request HTTP request
     * @return Parsed command payload
     */
    SlashCommandPayload parsePayload(HttpRequest request);

    /**
     * Get command name
     * @return Command name (without /)
     */
    String getCommandName();
}
```

### IInteractionRouteHandler

```java
public interface IInteractionRouteHandler {
    /**
     * Handle interactive component interaction
     * @param request HTTP request
     * @return HTTP response
     */
    HttpResponse handleInteraction(HttpRequest request);

    /**
     * Validate interaction request
     * @param request HTTP request
     * @return Validation result
     */
    RequestValidationResult validate(HttpRequest request);

    /**
     * Parse interaction payload
     * @param request HTTP request
     * @return Parsed interaction payload
     */
    InteractionPayload parsePayload(HttpRequest request);

    /**
     * Get supported action IDs
     * @return Set of action IDs this handler processes
     */
    Set<String> getSupportedActionIds();
}
```

### IOAuthRouteHandler

```java
public interface IOAuthRouteHandler {
    /**
     * Handle OAuth callback
     * @param request HTTP request with authorization code
     * @return HTTP response (redirect or HTML page)
     */
    HttpResponse handleCallback(HttpRequest request);

    /**
     * Validate OAuth callback request
     * @param request HTTP request
     * @return Validation result
     */
    RequestValidationResult validate(HttpRequest request);

    /**
     * Extract authorization code
     * @param request HTTP request
     * @return Authorization code
     */
    String extractAuthorizationCode(HttpRequest request);

    /**
     * Extract state parameter
     * @param request HTTP request
     * @return State parameter
     */
    String extractState(HttpRequest request);
}
```

## Request/Response Objects

### HttpRequest

```java
public class HttpRequest {
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final String body;
    private final String remoteAddress;
    private final long timestamp;

    public HttpRequest(HttpMethod method, String path,
                      Map<String, String> headers,
                      Map<String, String> queryParams,
                      String body, String remoteAddress) {
        this.method = method;
        this.path = path;
        this.headers = Collections.unmodifiableMap(headers);
        this.queryParams = Collections.unmodifiableMap(queryParams);
        this.body = body;
        this.remoteAddress = remoteAddress;
        this.timestamp = System.currentTimeMillis();
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    // Getters...
}
```

### HttpResponse

```java
public class HttpResponse {
    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;
    private final String contentType;

    private HttpResponse(int statusCode, Map<String, String> headers,
                        String body, String contentType) {
        this.statusCode = statusCode;
        this.headers = Collections.unmodifiableMap(headers);
        this.body = body;
        this.contentType = contentType;
    }

    public static HttpResponse ok(String body) {
        return new HttpResponse(200, Collections.emptyMap(), body, "application/json");
    }

    public static HttpResponse ok(String body, String contentType) {
        return new HttpResponse(200, Collections.emptyMap(), body, contentType);
    }

    public static HttpResponse created(String body) {
        return new HttpResponse(201, Collections.emptyMap(), body, "application/json");
    }

    public static HttpResponse badRequest(String message) {
        return new HttpResponse(400, Collections.emptyMap(),
            "{\"error\":\"" + message + "\"}", "application/json");
    }

    public static HttpResponse unauthorized(String message) {
        return new HttpResponse(401, Collections.emptyMap(),
            "{\"error\":\"" + message + "\"}", "application/json");
    }

    public static HttpResponse notFound() {
        return new HttpResponse(404, Collections.emptyMap(),
            "{\"error\":\"Not found\"}", "application/json");
    }

    public static HttpResponse internalError() {
        return new HttpResponse(500, Collections.emptyMap(),
            "{\"error\":\"Internal server error\"}", "application/json");
    }

    public static HttpResponse redirect(String location) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", location);
        return new HttpResponse(302, headers, "", "text/html");
    }

    // Getters...
}
```

### SlashCommandPayload

```java
public class SlashCommandPayload {
    private final String token;
    private final String teamId;
    private final String teamDomain;
    private final String channelId;
    private final String channelName;
    private final String userId;
    private final String userName;
    private final String command;
    private final String text;
    private final String responseUrl;
    private final String triggerId;

    // Constructor from form data
    public static SlashCommandPayload fromFormData(Map<String, String> formData) {
        return new SlashCommandPayload(
            formData.get("token"),
            formData.get("team_id"),
            formData.get("team_domain"),
            formData.get("channel_id"),
            formData.get("channel_name"),
            formData.get("user_id"),
            formData.get("user_name"),
            formData.get("command"),
            formData.get("text"),
            formData.get("response_url"),
            formData.get("trigger_id")
        );
    }

    public String[] getArgs() {
        return text != null ? text.split("\\s+") : new String[0];
    }

    // Constructor and getters...
}
```

### InteractionPayload

```java
public class InteractionPayload {
    public enum InteractionType {
        BLOCK_ACTIONS("block_actions"),
        VIEW_SUBMISSION("view_submission"),
        VIEW_CLOSED("view_closed"),
        SHORTCUT("shortcut"),
        MESSAGE_ACTION("message_action");

        private final String value;
        InteractionType(String value) { this.value = value; }
    }

    private final InteractionType type;
    private final String token;
    private final String triggerId;
    private final User user;
    private final Team team;
    private final Channel channel;
    private final List<Action> actions;
    private final View view;
    private final Message message;
    private final String responseUrl;

    // Parse from JSON
    public static InteractionPayload fromJson(String json) {
        // Parse JSON payload
        // Implementation using Jackson or similar
    }

    // Getters...
}
```

## Security Interfaces

### IRequestValidator

```java
public interface IRequestValidator {
    /**
     * Validate request signature
     * @param request HTTP request
     * @param signingSecret Slack signing secret
     * @return true if signature valid
     */
    boolean validateSignature(HttpRequest request, String signingSecret);

    /**
     * Validate request timestamp
     * @param request HTTP request
     * @param maxAgeSeconds Maximum age in seconds
     * @return true if timestamp valid
     */
    boolean validateTimestamp(HttpRequest request, int maxAgeSeconds);

    /**
     * Validate request origin
     * @param request HTTP request
     * @return true if from localhost
     */
    boolean validateOrigin(HttpRequest request);

    /**
     * Compute expected signature
     * @param timestamp Request timestamp
     * @param body Request body
     * @param secret Signing secret
     * @return Expected signature
     */
    String computeSignature(String timestamp, String body, String secret);
}
```

### ISecurityMiddleware

```java
public interface ISecurityMiddleware {
    /**
     * Process request through security checks
     * @param request HTTP request
     * @param next Next handler in chain
     * @return HTTP response
     */
    HttpResponse process(HttpRequest request, RequestHandler next);

    /**
     * Check if request should be blocked
     * @param request HTTP request
     * @return Blocking reason or null if allowed
     */
    String shouldBlock(HttpRequest request);

    /**
     * Log security event
     * @param event Security event
     */
    void logSecurityEvent(SecurityEvent event);
}
```

## Configuration

### WebhookServerConfig

```java
public class WebhookServerConfig {
    // Server settings
    private final String host = "127.0.0.1";  // Localhost only
    private final int port;
    private final int maxThreads = 50;
    private final int minThreads = 10;
    private final int idleTimeout = 30000;

    // Security settings
    private final String signingSecret;
    private final int requestTimeoutSeconds = 5;
    private final int maxRequestSizeBytes = 10485760; // 10MB
    private final boolean validateOrigin = true;

    // Performance settings
    private final int requestQueueSize = 1000;
    private final boolean enableCompression = true;
    private final int connectionTimeout = 10000;

    // SSL settings (for future)
    private final boolean enableSsl = false;
    private final String keystorePath;
    private final String keystorePassword;

    // Builder pattern
    public static class Builder {
        private int port = 7395;
        private String signingSecret;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder signingSecret(String secret) {
            this.signingSecret = secret;
            return this;
        }

        public WebhookServerConfig build() {
            if (signingSecret == null) {
                throw new IllegalStateException("Signing secret required");
            }
            return new WebhookServerConfig(this);
        }
    }

    private WebhookServerConfig(Builder builder) {
        this.port = builder.port;
        this.signingSecret = builder.signingSecret;
        // Initialize other fields...
    }

    // Getters...
}
```

## Status and Metrics

### WebhookServerStatus

```java
public class WebhookServerStatus {
    public enum Status {
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        ERROR
    }

    private final Status status;
    private final String host;
    private final int port;
    private final long startTime;
    private final int activeConnections;
    private final int queuedRequests;
    private final String errorMessage;

    // Constructor and getters...

    public long getUptimeMs() {
        return status == Status.RUNNING || status == Status.STOPPING
            ? System.currentTimeMillis() - startTime
            : 0;
    }
}
```

### WebhookServerMetrics

```java
public class WebhookServerMetrics {
    private final long totalRequests;
    private final long successfulRequests;
    private final long failedRequests;
    private final long rejectedRequests;
    private final Map<Integer, Long> statusCodeCounts;
    private final double averageResponseTimeMs;
    private final long maxResponseTimeMs;
    private final long minResponseTimeMs;
    private final Map<String, Long> requestsByPath;
    private final long lastRequestTime;

    // Constructor and getters...

    public double getSuccessRate() {
        return totalRequests > 0
            ? (double) successfulRequests / totalRequests * 100
            : 0.0;
    }

    public double getErrorRate() {
        return 100.0 - getSuccessRate();
    }
}
```

### RequestValidationResult

```java
public class RequestValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final ValidationFailureReason reason;

    public enum ValidationFailureReason {
        INVALID_SIGNATURE,
        EXPIRED_TIMESTAMP,
        INVALID_ORIGIN,
        MISSING_HEADERS,
        MALFORMED_PAYLOAD,
        RATE_LIMITED
    }

    public RequestValidationResult(boolean valid, List<String> errors,
                                   ValidationFailureReason reason) {
        this.valid = valid;
        this.errors = Collections.unmodifiableList(errors);
        this.reason = reason;
    }

    public static RequestValidationResult success() {
        return new RequestValidationResult(true, Collections.emptyList(), null);
    }

    public static RequestValidationResult failure(ValidationFailureReason reason,
                                                  String... errors) {
        return new RequestValidationResult(false, Arrays.asList(errors), reason);
    }

    // Getters...
}
```

## Event Processing

### IEventProcessor

```java
public interface IEventProcessor {
    /**
     * Process Slack event asynchronously
     * @param event Slack event
     * @return Processing future
     */
    CompletableFuture<EventProcessingResult> processAsync(SlackEvent event);

    /**
     * Process event synchronously (for testing)
     * @param event Slack event
     * @return Processing result
     */
    EventProcessingResult processSync(SlackEvent event);

    /**
     * Get processing status
     * @param eventId Event ID
     * @return Processing status
     */
    Optional<EventProcessingStatus> getStatus(String eventId);

    /**
     * Cancel event processing
     * @param eventId Event ID
     * @return true if cancelled
     */
    boolean cancel(String eventId);
}
```

### EventProcessingResult

```java
public class EventProcessingResult {
    public enum Status {
        SUCCESS,
        FAILED,
        SKIPPED,
        CANCELLED
    }

    private final Status status;
    private final String eventId;
    private final long processingTimeMs;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    // Constructor and getters...
}
```

## Connection Management

### IConnectionManager

```java
public interface IConnectionManager {
    /**
     * Accept new connection
     * @param connection Connection to accept
     * @return true if accepted
     */
    boolean accept(Connection connection);

    /**
     * Close connection
     * @param connectionId Connection ID
     */
    void close(String connectionId);

    /**
     * Get active connections
     * @return List of active connections
     */
    List<Connection> getActiveConnections();

    /**
     * Get connection count
     * @return Number of active connections
     */
    int getConnectionCount();

    /**
     * Close idle connections
     * @param idleTimeoutMs Idle timeout in milliseconds
     * @return Number of connections closed
     */
    int closeIdleConnections(long idleTimeoutMs);
}
```

## Error Handling

### WebhookServerException

```java
public class WebhookServerException extends Exception {
    public enum ErrorType {
        STARTUP_ERROR,
        SHUTDOWN_ERROR,
        REQUEST_PROCESSING_ERROR,
        VALIDATION_ERROR,
        SECURITY_ERROR,
        CONFIGURATION_ERROR
    }

    private final ErrorType errorType;
    private final int statusCode;

    public WebhookServerException(ErrorType errorType, String message,
                                  Throwable cause, int statusCode) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }

    // Getters...
}
```

## Lifecycle Management

### IServerLifecycleListener

```java
public interface IServerLifecycleListener {
    void onStarting();
    void onStarted(int port);
    void onStartupFailed(Exception error);
    void onStopping();
    void onStopped();
    void onShutdownFailed(Exception error);
}
```

## Performance Requirements

### Latency
- Request processing: < 500ms (P95)
- Slack event acknowledgment: < 100ms
- Command response: < 3000ms (Slack requirement)
- OAuth callback: < 1000ms

### Throughput
- Requests per second: 100
- Concurrent connections: 50
- Queue capacity: 1000 requests

### Resource Limits
- Memory usage: < 50MB
- Thread pool: 10-50 threads
- Request body size: 10MB maximum

## Thread Safety

All implementations must be thread-safe for:
- Concurrent request handling
- Simultaneous route registration
- Metrics collection
- Connection management

## Testing Requirements

### Unit Tests
- Test request validation
- Test signature verification
- Test route handling
- Test error responses

### Integration Tests
- Test full Slack event flow
- Test command handling
- Test OAuth callback
- Test concurrent requests

### Performance Tests
- Load test with 100 RPS
- Test connection pool behavior
- Test queue overflow handling

## Security Requirements

### Request Security
- Signature verification on all requests
- Timestamp validation (5-minute window)
- Origin validation (localhost only)
- Request size limits

### Server Security
- Bind to localhost only (127.0.0.1)
- No CORS headers
- Rate limiting per endpoint
- Request logging (no sensitive data)

### Error Handling
- Generic error messages to clients
- Detailed logging internally
- No stack traces in responses
- Security event auditing

## Usage Examples

### Starting Server

```java
WebhookServerConfig config = new WebhookServerConfig.Builder()
    .port(7395)
    .signingSecret(signingSecret)
    .build();

IWebhookServer server = new WebhookServerImpl();
server.start(config);

// Register handlers
server.registerEventRoute("/slack/events", eventHandler);
server.registerCommandRoute("/slack/commands", commandHandler);
server.registerInteractionRoute("/slack/interactive", interactionHandler);
server.registerOAuthRoute("/oauth/callback", oauthHandler);
```

### Handling Events

```java
public class MessageEventHandler implements IEventRouteHandler {
    @Override
    public HttpResponse handleEvent(HttpRequest request) {
        // Validate request
        RequestValidationResult validation = validate(request);
        if (!validation.isValid()) {
            return HttpResponse.unauthorized("Invalid request");
        }

        // Parse event
        SlackEvent event = parseEvent(request.getBody());

        // Acknowledge immediately
        if (event.getType() == SlackEventType.URL_VERIFICATION) {
            return HttpResponse.ok("{\"challenge\":\"" +
                event.getChallenge() + "\"}");
        }

        // Process asynchronously
        eventProcessor.processAsync(event);

        // Return 200 OK within 3 seconds
        return HttpResponse.ok("");
    }

    @Override
    public String getPath() {
        return "/slack/events";
    }

    // Other methods...
}
```

### Validating Requests

```java
public class SlackRequestValidator implements IRequestValidator {
    @Override
    public boolean validateSignature(HttpRequest request, String signingSecret) {
        String timestamp = request.getHeader("X-Slack-Request-Timestamp");
        String signature = request.getHeader("X-Slack-Signature");

        if (timestamp == null || signature == null) {
            return false;
        }

        // Verify timestamp is within 5 minutes
        if (!validateTimestamp(request, 300)) {
            return false;
        }

        // Compute expected signature
        String expected = computeSignature(timestamp, request.getBody(), signingSecret);

        // Constant-time comparison
        return MessageDigest.isEqual(
            signature.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public String computeSignature(String timestamp, String body, String secret) {
        String baseString = "v0:" + timestamp + ":" + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        );
        mac.init(keySpec);
        byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
        return "v0=" + Hex.encodeHexString(hash);
    }

    // Other methods...
}
```