# Task Interface: Sprint 1 Infrastructure Setup

## Overview
This document defines the public APIs and interfaces for the Sprint 1 infrastructure components of SlackGrab. These components provide the foundation for all future development.

## Public APIs

### 1. SlackGrabApplication
**Location**: `com.slackgrab.SlackGrabApplication`

Main application entry point. Manages lifecycle and dependency injection.

**Public Methods**:
- `void start()` - Starts all application services
- `void stop()` - Gracefully shuts down all services
- `static void main(String[] args)` - Application entry point

**Example Usage**:
```java
SlackGrabApplication app = new SlackGrabApplication();
app.start();
// Application runs...
app.shutdown();
```

### 2. ConfigurationManager
**Location**: `com.slackgrab.core.ConfigurationManager`

Manages internal application configuration (NOT user-facing).

**Public Methods**:
- `Path getAppDataPath()` - Returns: `%LOCALAPPDATA%\SlackGrab`
- `Path getLogsPath()` - Returns: `%LOCALAPPDATA%\SlackGrab\logs`
- `Path getDatabasePath()` - Returns: `%LOCALAPPDATA%\SlackGrab\database`
- `Path getCachePath()` - Returns: `%LOCALAPPDATA%\SlackGrab\cache`
- `Path getModelsPath()` - Returns: `%LOCALAPPDATA%\SlackGrab\models`
- `int getWebhookPort()` - Returns: `7395`
- `String getWebhookHost()` - Returns: `localhost`
- `long getMaxMemoryMB()` - Returns: `4096`
- `double getMaxCpuPercent()` - Returns: `5.0`
- `double getMaxGpuMemoryPercent()` - Returns: `80.0`
- `double getCpuThrottleThreshold()` - Returns: `80.0`
- `int getMaxMessagesPerDay()` - Returns: `5000`
- `int getMaxChannels()` - Returns: `2000`
- `int getHistoricalDataDays()` - Returns: `30`
- `long getScoringLatencyMs()` - Returns: `1000`
- `long getApiResponseMs()` - Returns: `100`

**Example Usage**:
```java
@Inject
ConfigurationManager config;

Path dbPath = config.getDatabasePath();
int webhookPort = config.getWebhookPort();
```

### 3. ErrorHandler
**Location**: `com.slackgrab.core.ErrorHandler`

Centralized silent error management.

**Public Methods**:
- `void handleError(String message, Throwable error)` - Log and continue
- `void handleCriticalError(String message, Throwable error)` - Log and attempt shutdown
- `boolean handleRecoverableError(String message, Throwable error)` - Returns true if should retry
- `void handleWarning(String message)` - Log warning
- `void handleDegradedMode(String message, Throwable error)` - Continue with reduced features

**Example Usage**:
```java
@Inject
ErrorHandler errorHandler;

try {
    // risky operation
} catch (Exception e) {
    errorHandler.handleError("Operation failed", e);
    // Continue without interrupting user
}
```

### 4. DatabaseManager
**Location**: `com.slackgrab.data.DatabaseManager`

SQLite database management with schema initialization.

**Public Methods**:
- `void start() throws Exception` - Initialize database connection and schema
- `void stop() throws Exception` - Close database connection
- `Connection getConnection() throws SQLException` - Get active database connection
- `boolean isReady()` - Check if database is available

**Database Schema**:
- `messages` - Message content and metadata
- `user_interactions` - User behavior tracking (for ML)
- `feedback` - Three-level feedback data
- `channels` - Channel information
- `system_state` - System state key-value storage

**Example Usage**:
```java
@Inject
DatabaseManager dbManager;

Connection conn = dbManager.getConnection();
try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages")) {
    ResultSet rs = stmt.executeQuery();
    // process results
}
```

### 5. CredentialManager
**Location**: `com.slackgrab.security.CredentialManager`

Secure token storage (currently in-memory, will use Windows Credential Manager).

**Public Methods**:
- `boolean storeAccessToken(String token)` - Store Slack access token
- `Optional<String> getAccessToken()` - Retrieve access token
- `boolean storeRefreshToken(String token)` - Store refresh token
- `Optional<String> getRefreshToken()` - Retrieve refresh token
- `boolean storeWorkspaceId(String workspaceId)` - Store workspace ID
- `Optional<String> getWorkspaceId()` - Retrieve workspace ID
- `boolean deleteAllCredentials()` - Clear all stored credentials
- `boolean hasAccessToken()` - Check if access token exists

**Example Usage**:
```java
@Inject
CredentialManager credentialManager;

credentialManager.storeAccessToken("xoxb-your-token");
Optional<String> token = credentialManager.getAccessToken();
if (token.isPresent()) {
    // Use token
}
```

### 6. WebhookServer
**Location**: `com.slackgrab.webhook.WebhookServer`

Localhost webhook server for Slack events.

**Public Methods**:
- `void start() throws Exception` - Start webhook server on localhost:7395
- `void stop() throws Exception` - Stop webhook server
- `boolean isRunning()` - Check if server is running

**Endpoints**:
- `GET /health` - Health check endpoint
  - Response: `{"status": "ok", "timestamp": <unix-timestamp>}`
- `POST /slack/events` - Slack event webhook
- `POST /slack/interactive` - Slack interactive components
- `POST /slack/commands` - Slack slash commands

**Example Usage**:
```java
@Inject
WebhookServer webhookServer;

webhookServer.start();
// Server running on localhost:7395
```

### 7. SlackApiClient
**Location**: `com.slackgrab.slack.SlackApiClient`

Slack API client wrapper with token management.

**Public Methods**:
- `void setAccessToken(String token)` - Set and store access token
- `boolean hasAccessToken()` - Check if token is available
- `boolean testConnection()` - Test Slack API connection
- `Slack getSlack()` - Get underlying Slack client instance
- `Optional<String> getAccessToken()` - Get current access token
- `void clearAccessToken()` - Clear stored token

**Example Usage**:
```java
@Inject
SlackApiClient slackClient;

slackClient.setAccessToken("xoxb-your-token");
if (slackClient.testConnection()) {
    // API is accessible
    Slack slack = slackClient.getSlack();
    // Use Slack SDK
}
```

### 8. ServiceCoordinator
**Location**: `com.slackgrab.core.ServiceCoordinator`

Manages lifecycle of all application services.

**Public Methods**:
- `void start() throws Exception` - Start all services in order
- `void shutdown()` - Stop all services in reverse order
- `boolean isStarted()` - Check if services are running

**Service Startup Order**:
1. DatabaseManager
2. WebhookServer

**Example Usage**:
```java
@Inject
ServiceCoordinator coordinator;

coordinator.start(); // Starts all services
// Application running...
coordinator.shutdown(); // Stops all services
```

## Data Structures

### HealthResponse (WebhookServer)
```java
record HealthResponse(String status, long timestamp) {}
```

## Integration Points

### For Future ML Components
- Use `DatabaseManager.getConnection()` to access training data from `user_interactions` and `feedback` tables
- Resource limits available via `ConfigurationManager.getMaxGpuMemoryPercent()`, etc.

### For Future Slack Integration
- Use `SlackApiClient` for all Slack API calls
- Use `CredentialManager` for token storage
- Subscribe to webhook endpoints for real-time events

### For Future UI Components
- All errors handled silently via `ErrorHandler` - no popups needed
- Configuration is internal only - no user-facing settings

## Error Handling Contract

All components use `ErrorHandler` for silent operation:
- **Never** throw exceptions to user
- **Always** log errors to `%LOCALAPPDATA%\SlackGrab\logs\slackgrab.log`
- **Continue** operating with degraded functionality when possible

## Configuration Contract

Zero configuration principle:
- No user-facing configuration options
- All settings are internal with moderate defaults
- System self-tunes through behavioral observation (future)

## Testing Interface

All managed services implement `ManagedService` interface:
```java
public interface ManagedService {
    void start() throws Exception;
    void stop() throws Exception;
}
```

## Dependencies

### Runtime Dependencies
- Java 25+
- Windows 11+ (for LOCALAPPDATA environment variable)
- SQLite JDBC driver
- Slack Java SDK
- Javalin web framework
- Google Guice (dependency injection)
- SLF4J + Logback (logging)

### Optional Dependencies
- JNA (for future Windows Credential Manager integration)
- Bouncy Castle (for future encryption)

## Migration Notes

### Future Enhancements
1. **CredentialManager**: Currently uses in-memory storage. Will be migrated to Windows Credential Manager using JNA.
2. **DatabaseManager**: Currently uses plain SQLite. Will add SQLCipher encryption for data at rest.
3. **WebhookServer**: Will add Slack signature verification for security.

## Versioning

This interface is for Sprint 1 infrastructure. Breaking changes will be documented in future sprint interfaces.
