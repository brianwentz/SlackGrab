# Task Interface: OAuth Sign-In Flow and Application Exit Fixes

## Public APIs

### SystemTrayManager

**Constructor Injection:**
```java
@Inject
public SystemTrayManager(
    ErrorHandler errorHandler,
    AutoStartManager autoStartManager,
    StatusWindow statusWindow,
    ServiceCoordinator serviceCoordinator,
    OAuthManager oauthManager,
    CredentialManager credentialManager
)
```

**Public Methods:**

```java
// ManagedService interface implementation
void start() throws Exception
void stop() throws Exception
boolean isStarted()

// OAuth success callback (called by WebhookServer)
void onOAuthSuccess()

// Notification methods (existing)
void showNotification(String title, String message, TrayIcon.MessageType messageType)
void showErrorNotification(String message)
void showInfoNotification(String message)
```

**Dependencies:**
- `ServiceCoordinator`: Required for graceful application shutdown
- `OAuthManager`: Required for OAuth URL generation
- `CredentialManager`: Required for checking connection status
- `ErrorHandler`: Required for error handling
- `AutoStartManager`: Required for auto-start toggle
- `StatusWindow`: Required for status display

**Expected Behavior:**
- `start()`: Initializes system tray, checks connection status, shows welcome notification if not connected
- `onOAuthSuccess()`: Updates tray tooltip and menu after successful OAuth, shows success notification
- Exit menu item: Removes tray icon, shuts down all services via ServiceCoordinator, exits JVM

### WebhookServer

**Constructor Injection:**
```java
@Inject
public WebhookServer(
    ConfigurationManager configurationManager,
    ErrorHandler errorHandler,
    OAuthManager oauthManager
)
```

**Setter Injection (to avoid circular dependency):**
```java
@Inject
public void setSystemTrayManager(SystemTrayManager systemTrayManager)
```

**OAuth Callback Endpoint:**
```
GET /slack/oauth/callback?code={authorization_code}&state={state_token}
```

**Response:**
- Success: HTML success page, triggers `systemTrayManager.onOAuthSuccess()`
- Error: HTML error page with user-friendly message

**Dependencies:**
- `SystemTrayManager`: Optional, used to notify tray of successful OAuth

### StatusWindow

**Constructor Injection:**
```java
@Inject
public StatusWindow(
    ErrorHandler errorHandler,
    DatabaseManager databaseManager,
    CredentialManager credentialManager
)
```

**Public Methods:**
```java
void show()  // Display status window
void hide()  // Hide status window
boolean isVisible()
```

**Display Fields:**
- Connection Status: "Connected to Slack" (green) or "Not Connected" (red)
- Workspace: Team/Workspace ID or "Not Connected"
- Last Sync: Timestamp of last message sync
- Messages Collected: Total message count
- Current State: "Running" (green) or other operational state

**Dependencies:**
- `CredentialManager`: Required for checking connection status and workspace ID

## Data Structures

### OAuth Flow State

```java
// OAuthManager.OAuthResult (existing)
public record OAuthResult(
    String accessToken,
    String refreshToken,
    String teamId,
    String teamName,
    String scope,
    String botUserId
)
```

### Credential Storage

**Registry Keys:**
- `HKEY_CURRENT_USER\Software\SlackGrab\Credentials\AccessToken`
- `HKEY_CURRENT_USER\Software\SlackGrab\Credentials\RefreshToken`
- `HKEY_CURRENT_USER\Software\SlackGrab\Credentials\TeamId`
- `HKEY_CURRENT_USER\Software\SlackGrab\Credentials\WorkspaceId`

**Values:** Base64-encoded strings

### System Tray Menu Structure

```
[Connect to Slack]  (only if not connected)
────────────────
Show Status
────────────────
☑ Start on Windows Login
────────────────
Exit
```

## Integration Points

### Component Communication Flow

**OAuth Sign-In Flow:**
```
User clicks "Connect to Slack"
    ↓
SystemTrayManager.initiateOAuthFlow()
    ↓
OAuthManager.generateAuthorizationUrl()
    ↓
Desktop.browse(authUrl) - Opens browser
    ↓
User authorizes in Slack
    ↓
Slack redirects to localhost:7395/slack/oauth/callback
    ↓
WebhookServer receives callback
    ↓
OAuthManager.exchangeCodeForToken(code)
    ↓
CredentialManager stores tokens
    ↓
WebhookServer calls systemTrayManager.onOAuthSuccess()
    ↓
SystemTrayManager updates menu and tooltip
```

**Application Exit Flow:**
```
User clicks "Exit" in tray menu
    ↓
SystemTrayManager.exitApplication()
    ↓
systemTray.remove(trayIcon)
    ↓
serviceCoordinator.shutdown()
    ↓
Services stop in reverse order:
  - SystemTrayManager.stop()
  - WebhookServer.stop()
  - DatabaseManager.stop()
    ↓
System.exit(0)
```

**Connection Status Check on Startup:**
```
SystemTrayManager.start()
    ↓
checkConnectionStatus()
    ↓
credentialManager.hasAccessToken()
    ↓
if (has token):
    updateTrayTooltip("SlackGrab - Connected")
    showInfoNotification("Connected to Slack workspace")
else:
    updateTrayTooltip("SlackGrab - Not Connected")
    showWelcomeNotification()
```

### Other Components Should Use This

**To Trigger OAuth Flow Programmatically:**
Not recommended - OAuth should be user-initiated. However, if needed:
```java
// Not a public API, but this is the internal flow
systemTrayManager.onOAuthSuccess();  // Call after successful OAuth
```

**To Check Connection Status:**
```java
boolean isConnected = credentialManager.hasAccessToken();
Optional<String> teamId = credentialManager.getTeamId();
```

**To Trigger Application Shutdown:**
```java
serviceCoordinator.shutdown();  // Graceful shutdown
System.exit(0);                  // Exit JVM
```

**To Show Notifications:**
```java
systemTrayManager.showInfoNotification("Message");
systemTrayManager.showErrorNotification("Error message");
systemTrayManager.showNotification("Title", "Message", TrayIcon.MessageType.INFO);
```

## Example Usage

### Starting the Application with OAuth Support

```java
public class SlackGrabApplication {
    public static void main(String[] args) {
        SlackGrabApplication app = new SlackGrabApplication();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));

        // Start application
        app.start();  // This initializes SystemTrayManager which checks OAuth status

        // Keep running
        Thread.currentThread().join();
    }
}
```

### Handling OAuth Success in Custom Components

```java
public class CustomComponent {
    @Inject
    private SystemTrayManager systemTrayManager;

    public void onOAuthComplete() {
        // After successful OAuth token exchange
        systemTrayManager.onOAuthSuccess();
    }
}
```

### Checking Connection Status

```java
public class MessageCollector {
    @Inject
    private CredentialManager credentialManager;

    public void startCollection() {
        if (!credentialManager.hasAccessToken()) {
            logger.warn("Cannot start collection - not connected to Slack");
            return;
        }

        Optional<String> teamId = credentialManager.getTeamId();
        logger.info("Starting collection for workspace: {}", teamId.orElse("unknown"));

        // Proceed with message collection
    }
}
```

### Displaying Status Window

```java
public class AnyComponent {
    @Inject
    private StatusWindow statusWindow;

    public void showStatus() {
        statusWindow.show();  // Display status window
    }
}
```

### Graceful Shutdown

```java
public class AnyComponent {
    @Inject
    private ServiceCoordinator serviceCoordinator;

    public void shutdown() {
        // Shutdown all services gracefully
        serviceCoordinator.shutdown();

        // Exit application
        System.exit(0);
    }
}
```

## Error Scenarios

### OAuth Configuration Error

**Trigger:** `SLACK_CLIENT_ID` or `SLACK_CLIENT_SECRET` not set

**Response:**
```
Notification: "Configuration Error - Slack OAuth credentials not configured.
              Please set SLACK_CLIENT_ID and SLACK_CLIENT_SECRET environment variables."
MessageType: ERROR
```

### Browser Launch Error

**Trigger:** Desktop browsing not supported on platform

**Response:**
```
Notification: "Browser Error - Unable to open browser automatically.
              Please visit the OAuth URL manually."
MessageType: ERROR
```

### OAuth Token Exchange Error

**Trigger:** Authorization code invalid or expired

**Response:**
```
Browser Page: "Token Exchange Failed - Failed to complete authorization: [error details]"
Logs: Error details logged for debugging
```

### Shutdown Error

**Trigger:** Service fails to stop gracefully

**Response:**
```
Logs: "Error stopping service: [ServiceName]"
Action: Continue shutting down other services
Fallback: Runtime.getRuntime().halt(1) if System.exit() fails
```

### Network Error During OAuth

**Trigger:** Cannot reach Slack OAuth endpoint

**Response:**
```
Browser Page: "Unexpected Error - An unexpected error occurred during authorization"
Logs: Full stack trace logged
```

## Dependencies Between Components

```
SystemTrayManager
    ├── ServiceCoordinator (required for shutdown)
    ├── OAuthManager (required for OAuth URL)
    ├── CredentialManager (required for status check)
    ├── ErrorHandler (required for error handling)
    ├── AutoStartManager (required for auto-start)
    └── StatusWindow (required for status display)

WebhookServer
    ├── OAuthManager (required for token exchange)
    ├── ConfigurationManager (required for port/host)
    ├── ErrorHandler (required for error handling)
    └── SystemTrayManager (optional for OAuth success notification)

StatusWindow
    ├── CredentialManager (required for connection status)
    ├── DatabaseManager (required for message stats)
    └── ErrorHandler (required for error handling)

ServiceCoordinator
    ├── DatabaseManager (managed service)
    ├── WebhookServer (managed service)
    ├── SystemTrayManager (managed service)
    ├── ConfigurationManager (required for config)
    └── ErrorHandler (required for error handling)
```

## Threading Considerations

- **System Tray Events:** Run on AWT Event Dispatch Thread
- **OAuth Callback:** Runs on Javalin HTTP thread
- **Service Shutdown:** Runs on thread that calls shutdown (usually shutdown hook thread)
- **Notifications:** Display on AWT EDT via `trayIcon.displayMessage()`

**Thread Safety:**
- All state changes in SystemTrayManager are synchronized via AWT EDT
- ServiceCoordinator.shutdown() is synchronized to prevent concurrent calls
- CredentialManager uses Windows Registry which provides its own synchronization

## Configuration Requirements

### Environment Variables

```bash
# Required for OAuth flow
SLACK_CLIENT_ID=xoxb-your-client-id
SLACK_CLIENT_SECRET=your-client-secret
```

### Slack App Configuration

**Required OAuth Scopes:**
- `channels:history` - Read public channel messages
- `channels:read` - List public channels
- `groups:history` - Read private channels
- `groups:read` - List private channels
- `im:history` - Read direct messages
- `im:read` - List direct messages
- `mpim:history` - Read group DMs
- `mpim:read` - List group DMs
- `users:read` - Get user information
- `team:read` - Get team information

**Redirect URL:**
```
http://localhost:7395/slack/oauth/callback
```

Must be configured in Slack App settings.

## Testing Interfaces

### Manual Testing

**Test OAuth Flow:**
1. Clear credentials: Delete `HKEY_CURRENT_USER\Software\SlackGrab\Credentials`
2. Start application
3. Right-click tray icon
4. Click "Connect to Slack"
5. Verify browser opens to Slack OAuth page
6. Authorize application
7. Verify redirect to localhost callback
8. Verify success page displays
9. Verify tray tooltip shows "SlackGrab - Connected"
10. Verify menu no longer shows "Connect to Slack"

**Test Application Exit:**
1. Start application
2. Right-click tray icon
3. Click "Exit"
4. Verify notification or logs show graceful shutdown
5. Check Task Manager - process should be gone
6. Check logs - should show all services stopped

**Test Status Window:**
1. Start application
2. Connect to Slack (if not connected)
3. Right-click tray icon → "Show Status"
4. Verify window shows:
   - Connection Status: Connected to Slack (green)
   - Workspace: [Team ID]
   - Last Sync: [timestamp]
   - Messages Collected: [count]
   - Current State: Running (green)

### Automated Testing Hooks

```java
// Check if connected
boolean isConnected = credentialManager.hasAccessToken();

// Check if menu shows Connect option
// (Would need to expose menu for testing - not currently public)

// Check service coordinator state
boolean servicesRunning = serviceCoordinator.isStarted();

// Trigger shutdown programmatically
serviceCoordinator.shutdown();
```

## Backward Compatibility

**Breaking Changes:** None

**New Features:**
- OAuth sign-in flow (new feature, no existing behavior changed)
- Improved exit behavior (enhancement, no API changes)
- Enhanced status window (UI improvement, no API changes)

**Migration:** No migration needed. Existing installations will see new "Connect to Slack" menu item if no credentials exist.

## Future Enhancements

**Potential API Additions:**
```java
// SystemTrayManager
void onOAuthFailure(String reason)  // Handle OAuth failures
void updateConnectionStatus(boolean connected, String workspaceName)  // Dynamic updates

// WebhookServer
void registerOAuthCallback(OAuthCallback callback)  // Custom callback handlers

// StatusWindow
void refresh()  // Force status refresh
void setWorkspaceName(String name)  // Update workspace display
```

These are not yet implemented but may be added in future iterations.
