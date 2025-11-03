# Slack Integration Interface Contract

## Overview
This contract defines all interfaces for Slack API integration, OAuth management, webhook handling, and Slack Apps platform integration.

## Core Interfaces

### ISlackApiClient

```java
public interface ISlackApiClient {
    /**
     * Initialize the client with authentication tokens
     * @param credentials OAuth credentials
     * @return true if initialization successful
     */
    boolean initialize(SlackCredentials credentials);

    /**
     * Test the API connection
     * @return Connection test result
     */
    ConnectionTestResult testConnection();

    /**
     * Fetch list of channels user has access to
     * @param types Channel types to fetch (public, private, im, mpim)
     * @param cursor Pagination cursor
     * @return Channel list response
     */
    CompletableFuture<ChannelListResponse> fetchChannels(
        Set<ChannelType> types,
        String cursor
    );

    /**
     * Fetch message history for a channel
     * @param channelId Channel ID
     * @param oldest Oldest timestamp to fetch
     * @param limit Maximum messages to fetch
     * @param cursor Pagination cursor
     * @return Message history response
     */
    CompletableFuture<MessageHistoryResponse> fetchHistory(
        String channelId,
        long oldest,
        int limit,
        String cursor
    );

    /**
     * Fetch user information
     * @param userId User ID
     * @return User information
     */
    CompletableFuture<User> fetchUser(String userId);

    /**
     * Post message to channel
     * @param channelId Channel ID
     * @param message Message content
     * @return Posted message response
     */
    CompletableFuture<MessageResponse> postMessage(
        String channelId,
        SlackMessage message
    );

    /**
     * Update App Home tab view
     * @param userId User ID
     * @param view Home tab view
     * @return View publish response
     */
    CompletableFuture<ViewResponse> updateHomeTab(
        String userId,
        HomeTabView view
    );

    /**
     * Get current rate limit status
     * @return Rate limit status
     */
    RateLimitStatus getRateLimitStatus();

    /**
     * Shutdown client and cleanup resources
     */
    void shutdown();
}
```

### IOAuthManager

```java
public interface IOAuthManager {
    /**
     * Generate OAuth authorization URL
     * @param redirectUri Redirect URI after authorization
     * @param scopes Required scopes
     * @return Authorization URL and state token
     */
    OAuthUrlResponse generateAuthorizationUrl(
        String redirectUri,
        List<String> scopes
    );

    /**
     * Exchange authorization code for access tokens
     * @param code Authorization code from callback
     * @param state State token for CSRF protection
     * @param codeVerifier PKCE code verifier
     * @return OAuth tokens
     */
    CompletableFuture<OAuthTokens> exchangeCodeForTokens(
        String code,
        String state,
        String codeVerifier
    );

    /**
     * Refresh access token
     * @param refreshToken Refresh token
     * @return New OAuth tokens
     */
    CompletableFuture<OAuthTokens> refreshAccessToken(String refreshToken);

    /**
     * Store tokens securely
     * @param tokens OAuth tokens to store
     */
    void storeTokens(OAuthTokens tokens);

    /**
     * Retrieve stored tokens
     * @return Stored OAuth tokens or null if not found
     */
    OAuthTokens retrieveTokens();

    /**
     * Revoke access tokens
     * @return true if revocation successful
     */
    CompletableFuture<Boolean> revokeTokens();

    /**
     * Check if tokens are valid and not expired
     * @return Token validity status
     */
    boolean areTokensValid();
}
```

### IWebhookServer

```java
public interface IWebhookServer {
    /**
     * Start the webhook server
     * @param port Port to listen on (default 7395)
     * @return true if server started successfully
     */
    boolean start(int port);

    /**
     * Stop the webhook server
     */
    void stop();

    /**
     * Register event handler
     * @param eventType Event type to handle
     * @param handler Event handler
     */
    void registerEventHandler(SlackEventType eventType, ISlackEventHandler handler);

    /**
     * Register slash command handler
     * @param command Command name
     * @param handler Command handler
     */
    void registerCommandHandler(String command, ISlashCommandHandler handler);

    /**
     * Register interactive component handler
     * @param actionId Action ID
     * @param handler Interaction handler
     */
    void registerInteractionHandler(String actionId, IInteractionHandler handler);

    /**
     * Get server status
     * @return Server status
     */
    ServerStatus getStatus();

    /**
     * Get server port
     * @return Port number
     */
    int getPort();

    /**
     * Check if server is running
     * @return true if running
     */
    boolean isRunning();
}
```

### IRateLimiter

```java
public interface IRateLimiter {
    /**
     * Request permission to make API call
     * @param method API method name
     * @return Future that completes when call is allowed
     */
    CompletableFuture<Void> requestPermission(String method);

    /**
     * Record API call result
     * @param method API method name
     * @param response API response with rate limit headers
     */
    void recordCall(String method, ApiResponse response);

    /**
     * Get wait time for method
     * @param method API method name
     * @return Wait time in milliseconds
     */
    long getWaitTime(String method);

    /**
     * Get current rate limit status
     * @param method API method name
     * @return Rate limit status
     */
    RateLimitInfo getRateLimit(String method);

    /**
     * Reset rate limiter (for testing)
     */
    void reset();
}
```

## Event Handler Interfaces

### ISlackEventHandler

```java
public interface ISlackEventHandler {
    /**
     * Handle Slack event
     * @param event The event payload
     * @return Event handling result
     */
    CompletableFuture<EventHandlingResult> handleEvent(SlackEvent event);

    /**
     * Get event type this handler processes
     * @return Event type
     */
    SlackEventType getEventType();

    /**
     * Priority for handling (higher = earlier)
     * @return Handler priority
     */
    int getPriority();
}
```

### ISlashCommandHandler

```java
public interface ISlashCommandHandler {
    /**
     * Handle slash command
     * @param command Command payload
     * @return Command response
     */
    CompletableFuture<SlashCommandResponse> handleCommand(SlashCommandPayload command);

    /**
     * Get command name this handler processes
     * @return Command name (without /)
     */
    String getCommandName();

    /**
     * Validate command before handling
     * @param command Command payload
     * @return Validation result
     */
    ValidationResult validate(SlashCommandPayload command);
}
```

### IInteractionHandler

```java
public interface IInteractionHandler {
    /**
     * Handle interactive component interaction
     * @param interaction Interaction payload
     * @return Interaction response
     */
    CompletableFuture<InteractionResponse> handleInteraction(
        InteractionPayload interaction
    );

    /**
     * Get action ID this handler processes
     * @return Action ID
     */
    String getActionId();

    /**
     * Maximum processing time in milliseconds
     * @return Timeout value
     */
    long getTimeout();
}
```

## Data Transfer Objects

### SlackCredentials

```java
public class SlackCredentials {
    private final String botToken;
    private final String userToken;
    private final String refreshToken;
    private final long expiresAt;
    private final String teamId;
    private final String botUserId;

    public SlackCredentials(String botToken, String userToken,
                           String refreshToken, long expiresAt,
                           String teamId, String botUserId) {
        this.botToken = botToken;
        this.userToken = userToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.teamId = teamId;
        this.botUserId = botUserId;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    // Getters...
}
```

### ChannelListResponse

```java
public class ChannelListResponse {
    private final List<Channel> channels;
    private final String nextCursor;
    private final boolean hasMore;
    private final ResponseMetadata metadata;

    public ChannelListResponse(List<Channel> channels, String nextCursor,
                               boolean hasMore, ResponseMetadata metadata) {
        this.channels = Collections.unmodifiableList(channels);
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.metadata = metadata;
    }

    // Getters...
}
```

### MessageHistoryResponse

```java
public class MessageHistoryResponse {
    private final List<Message> messages;
    private final String nextCursor;
    private final boolean hasMore;
    private final int rateLimit;
    private final int rateLimitRemaining;

    // Constructor and getters...
}
```

### OAuthTokens

```java
public class OAuthTokens {
    private final String accessToken;
    private final String tokenType;
    private final String scope;
    private final String botUserId;
    private final String teamId;
    private final String teamName;
    private final String userId;
    private final String userAccessToken;
    private final String refreshToken;
    private final long expiresIn;
    private final long timestamp;

    // Constructor and getters...

    public boolean needsRefresh() {
        long expiresAt = timestamp + (expiresIn * 1000);
        long fiveMinutes = 5 * 60 * 1000;
        return System.currentTimeMillis() > (expiresAt - fiveMinutes);
    }
}
```

### SlackEvent

```java
public class SlackEvent {
    private final SlackEventType type;
    private final String eventId;
    private final long eventTime;
    private final String teamId;
    private final String userId;
    private final Map<String, Object> payload;

    // Constructor and getters...

    public <T> T getPayloadAs(Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(payload, clazz);
    }
}
```

### SlackMessage

```java
public class SlackMessage {
    private final String text;
    private final List<Block> blocks;
    private final List<Attachment> attachments;
    private final String threadTs;
    private final boolean asUser;
    private final String iconEmoji;
    private final String username;

    // Builder pattern
    public static class Builder {
        private String text;
        private List<Block> blocks = new ArrayList<>();
        private List<Attachment> attachments = new ArrayList<>();
        private String threadTs;
        private boolean asUser = false;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder addBlock(Block block) {
            this.blocks.add(block);
            return this;
        }

        public Builder threadTs(String threadTs) {
            this.threadTs = threadTs;
            return this;
        }

        public SlackMessage build() {
            return new SlackMessage(text, blocks, attachments, threadTs, asUser, null, null);
        }
    }
}
```

### HomeTabView

```java
public class HomeTabView {
    private final String type = "home";
    private final List<Block> blocks;
    private final String privateMetadata;
    private final String callbackId;
    private final String externalId;

    // Constructor and builder...
}
```

## Enumerations

### ChannelType

```java
public enum ChannelType {
    PUBLIC_CHANNEL("public_channel"),
    PRIVATE_CHANNEL("private_channel"),
    IM("im"),
    MPIM("mpim");

    private final String value;

    ChannelType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

### SlackEventType

```java
public enum SlackEventType {
    MESSAGE("message"),
    MESSAGE_CHANGED("message_changed"),
    MESSAGE_DELETED("message_deleted"),
    REACTION_ADDED("reaction_added"),
    REACTION_REMOVED("reaction_removed"),
    CHANNEL_CREATED("channel_created"),
    CHANNEL_ARCHIVE("channel_archive"),
    USER_CHANGE("user_change"),
    APP_HOME_OPENED("app_home_opened");

    private final String value;

    SlackEventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SlackEventType fromString(String value) {
        for (SlackEventType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: " + value);
    }
}
```

## Response Objects

### ConnectionTestResult

```java
public class ConnectionTestResult {
    private final boolean successful;
    private final String teamName;
    private final String botUserId;
    private final String error;
    private final long responseTimeMs;

    public ConnectionTestResult(boolean successful, String teamName,
                                String botUserId, String error, long responseTimeMs) {
        this.successful = successful;
        this.teamName = teamName;
        this.botUserId = botUserId;
        this.error = error;
        this.responseTimeMs = responseTimeMs;
    }

    // Getters...
}
```

### SlashCommandResponse

```java
public class SlashCommandResponse {
    public enum ResponseType {
        EPHEMERAL("ephemeral"),
        IN_CHANNEL("in_channel");

        private final String value;
        ResponseType(String value) { this.value = value; }
    }

    private final ResponseType responseType;
    private final String text;
    private final List<Block> blocks;
    private final List<Attachment> attachments;

    // Constructor and getters...
}
```

### InteractionResponse

```java
public class InteractionResponse {
    private final String text;
    private final List<Block> blocks;
    private final boolean replaceOriginal;
    private final boolean deleteOriginal;

    // Constructor and getters...
}
```

## Rate Limiting

### RateLimitInfo

```java
public class RateLimitInfo {
    private final int limit;
    private final int remaining;
    private final long resetAt;
    private final String method;

    public RateLimitInfo(int limit, int remaining, long resetAt, String method) {
        this.limit = limit;
        this.remaining = remaining;
        this.resetAt = resetAt;
        this.method = method;
    }

    public long getWaitTimeMs() {
        if (remaining > 0) {
            return 0;
        }
        long now = System.currentTimeMillis();
        return Math.max(0, resetAt - now);
    }

    public boolean isExhausted() {
        return remaining <= 0;
    }

    // Getters...
}
```

## Error Handling

### SlackApiException

```java
public class SlackApiException extends Exception {
    public enum ErrorType {
        AUTHENTICATION_ERROR,
        RATE_LIMITED,
        NOT_FOUND,
        PERMISSION_DENIED,
        INVALID_REQUEST,
        SERVER_ERROR,
        NETWORK_ERROR,
        TIMEOUT
    }

    private final ErrorType errorType;
    private final int statusCode;
    private final String slackError;
    private final Long retryAfter;

    public SlackApiException(ErrorType errorType, String message,
                            int statusCode, String slackError, Long retryAfter) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
        this.slackError = slackError;
        this.retryAfter = retryAfter;
    }

    public boolean isRecoverable() {
        return errorType == ErrorType.RATE_LIMITED ||
               errorType == ErrorType.NETWORK_ERROR ||
               errorType == ErrorType.TIMEOUT;
    }

    // Getters...
}
```

## Security Interfaces

### ISignatureValidator

```java
public interface ISignatureValidator {
    /**
     * Validate Slack request signature
     * @param signature X-Slack-Signature header
     * @param timestamp X-Slack-Request-Timestamp header
     * @param body Request body
     * @return true if signature valid
     */
    boolean validateSignature(String signature, String timestamp, String body);

    /**
     * Check if timestamp is within acceptable window
     * @param timestamp Request timestamp
     * @return true if timestamp valid
     */
    boolean validateTimestamp(String timestamp);

    /**
     * Get signing secret
     * @return Signing secret (for testing only)
     */
    String getSigningSecret();
}
```

## Callback Interfaces

### IApiCallCallback

```java
public interface IApiCallCallback {
    void onCallStart(String method, ApiRequest request);
    void onCallComplete(String method, ApiResponse response, long durationMs);
    void onCallError(String method, Exception error);
    void onRateLimitHit(String method, long waitTimeMs);
}
```

### IEventProcessingCallback

```java
public interface IEventProcessingCallback {
    void onEventReceived(SlackEvent event);
    void onEventProcessed(SlackEvent event, EventHandlingResult result);
    void onEventError(SlackEvent event, Exception error);
}
```

## Configuration

### SlackApiConfig

```java
public class SlackApiConfig {
    private final String clientId;
    private final String clientSecret;
    private final String signingSecret;
    private final String redirectUri;
    private final List<String> scopes;
    private final int connectTimeout = 10000;  // 10 seconds
    private final int readTimeout = 30000;     // 30 seconds
    private final int maxRetries = 3;
    private final boolean autoRefreshTokens = true;

    // Builder pattern...
}
```

## Performance Requirements

### Latency
- API calls: < 3000ms (P95)
- OAuth flow: < 5000ms
- Webhook response: < 500ms
- Rate limit calculation: < 10ms

### Throughput
- API calls: 50/minute per method
- Webhook events: 1000/minute
- Command responses: < 3 seconds

### Reliability
- Retry failed requests up to 3 times
- Queue events during outages
- Graceful degradation on rate limits

## Thread Safety

All implementations must be thread-safe for:
- Concurrent API calls
- Simultaneous webhook events
- Token refresh during active requests
- Rate limiter access

## Testing Requirements

### Unit Tests
- Mock Slack API responses
- Test OAuth flow steps
- Verify signature validation
- Test rate limiting logic

### Integration Tests
- Full OAuth flow
- Webhook event handling
- API error handling
- Rate limit behavior

### Performance Tests
- Concurrent API calls
- High-volume event processing
- Rate limiter under load

## Security Requirements

### Authentication
- PKCE for OAuth flow
- Secure token storage (Windows Credential Manager)
- Automatic token refresh
- Token revocation on logout

### Request Security
- Signature verification on all webhooks
- Timestamp validation (5-minute window)
- HTTPS only for API calls
- Localhost-only webhook server

### Data Protection
- No logging of tokens
- Encrypt credentials at rest
- No sensitive data in error messages
- Secure memory handling for secrets