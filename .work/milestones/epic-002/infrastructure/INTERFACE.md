# Task Interface: Epic 2 Infrastructure Features

## Overview

This document defines the public interfaces for the Epic 2 infrastructure features:
- System Tray Integration
- Windows Auto-Start
- Token Refresh Mechanism

These components enable SlackGrab to run as a background Windows application with automatic startup and resilient Slack API authentication.

---

## 1. System Tray Integration

### Public API: SystemTrayManager

**Package**: `com.slackgrab.ui`

**Implements**: `ManagedService`

#### Methods

```java
public class SystemTrayManager implements ManagedService {
    // Lifecycle management
    void start() throws Exception;
    void stop() throws Exception;

    // Notifications
    void showNotification(String title, String message, TrayIcon.MessageType messageType);
    void showErrorNotification(String message);
    void showInfoNotification(String message);

    // Status
    boolean isStarted();
}
```

#### Integration Points

**Service Coordinator**: SystemTrayManager is registered as a managed service and starts after database and webhook services.

**Dependency Injection**: Injected via Guice in ApplicationModule:
```java
bind(SystemTrayManager.class).in(Singleton.class);
```

**Usage Example**:
```java
@Inject
private SystemTrayManager systemTrayManager;

// Service coordinator starts it automatically
serviceCoordinator.start();

// Show notifications
systemTrayManager.showInfoNotification("SlackGrab is running");
systemTrayManager.showErrorNotification("Failed to sync messages");
```

#### System Tray Menu Structure

```
SlackGrab - Message Prioritization
├── Show Status
├── ──────────────
├── ☑ Start on Windows Login
├── ──────────────
└── Exit
```

**Menu Actions**:
- **Show Status**: Opens StatusWindow displaying connection status, last sync time, message count
- **Start on Windows Login**: Toggle checkbox that enables/disables auto-start
- **Exit**: Gracefully shuts down application

---

## 2. Status Window

### Public API: StatusWindow

**Package**: `com.slackgrab.ui`

#### Methods

```java
public class StatusWindow {
    // Visibility control
    void show();
    void hide();
    boolean isVisible();
}
```

#### Displayed Information

**Status Window Content**:
- **Connection Status**: Connected/Disconnected (green/red)
- **Last Sync**: Timestamp of last message sync
- **Messages Collected**: Total count of messages in database
- **Current State**: Running/Idle/Syncing/Error

#### Integration Points

**System Tray**: StatusWindow is shown when user clicks "Show Status" menu item or double-clicks tray icon.

**Dependency Injection**:
```java
bind(StatusWindow.class).in(Singleton.class);
```

**Usage Example**:
```java
@Inject
private StatusWindow statusWindow;

// Show status
statusWindow.show();

// Hide status
statusWindow.hide();
```

---

## 3. Windows Auto-Start

### Public API: AutoStartManager

**Package**: `com.slackgrab.ui`

#### Methods

```java
public class AutoStartManager {
    // Auto-start management
    boolean enableAutoStart();
    boolean disableAutoStart();

    // Status
    boolean isAutoStartEnabled();
    String getAutoStartCommand();
    boolean verifyAutoStart();
}
```

#### Registry Details

**Registry Location**: `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`

**Registry Key**: `SlackGrab`

**Registry Value Format**:
- For JAR: `"C:\...\javaw.exe" -jar "C:\...\SlackGrab.jar"`
- For EXE: `"C:\Program Files\SlackGrab\SlackGrab.exe"`

#### Integration Points

**System Tray**: Auto-start toggle is accessible via system tray menu checkbox.

**Dependency Injection**:
```java
bind(AutoStartManager.class).in(Singleton.class);
```

**Usage Example**:
```java
@Inject
private AutoStartManager autoStartManager;

// Enable auto-start
boolean success = autoStartManager.enableAutoStart();
if (success) {
    System.out.println("Auto-start enabled");
}

// Check status
boolean enabled = autoStartManager.isAutoStartEnabled();
System.out.println("Auto-start: " + enabled);

// Disable auto-start
autoStartManager.disableAutoStart();
```

#### Error Handling

- Returns `false` on failure (errors logged via ErrorHandler)
- Silent operation (no user-facing errors)
- Registry access failures are logged but don't crash application

---

## 4. Token Refresh Mechanism

### Public API: OAuthManager (Enhanced)

**Package**: `com.slackgrab.oauth`

#### New Methods

```java
public class OAuthManager {
    // Token refresh
    String refreshAccessToken() throws OAuthException;
    boolean isTokenExpired(Exception exception);

    // Existing methods
    String generateAuthorizationUrl();
    OAuthResult exchangeCodeForToken(String code) throws OAuthException;
    boolean hasValidCredentials();
    void clearCredentials();
    Optional<String> getAccessToken();
}
```

#### Token Expiration Detection

**Detects these Slack API errors as token expiration**:
- `invalid_auth`
- `token_expired`
- `token_revoked`
- `account_inactive`
- HTTP 401 in error message

#### Integration Points

**SlackApiClient**: Automatically wraps API calls with token refresh logic.

**Usage Example**:
```java
@Inject
private OAuthManager oAuthManager;

// Check if error is token expiration
try {
    // Slack API call
} catch (Exception e) {
    if (oAuthManager.isTokenExpired(e)) {
        String newToken = oAuthManager.refreshAccessToken();
        // Retry API call with new token
    }
}
```

---

## 5. Automatic Token Refresh in SlackApiClient

### Public API: SlackApiClient (Enhanced)

**Package**: `com.slackgrab.slack`

#### New Methods

```java
public class SlackApiClient {
    // Automatic token refresh wrapper
    <T> T executeWithTokenRefresh(Supplier<T> apiCall) throws IOException, SlackApiException;
    boolean testConnectionWithRefresh();

    // Existing methods
    boolean hasAccessToken();
    boolean testConnection();
    Slack getSlack();
    Optional<String> getAccessToken();
    void setAccessToken(String token);
    void clearAccessToken();
}
```

#### Automatic Retry Logic

**Flow**:
1. Execute API call with current token
2. If token expired (401 error):
   - Call OAuthManager.refreshAccessToken()
   - Update stored token
   - Retry API call with new token
3. Return API response

#### Integration Points

**Message Collection**: MessageCollector and other Slack API consumers should wrap calls with `executeWithTokenRefresh()`.

**Usage Example**:
```java
@Inject
private SlackApiClient slackApiClient;

// Automatic token refresh on API calls
AuthTestResponse response = slackApiClient.executeWithTokenRefresh(() -> {
    try {
        return slack.methods(token).authTest(req -> req);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
});

// Or use convenience method
boolean connected = slackApiClient.testConnectionWithRefresh();
```

---

## Data Structures

### OAuthResult (Existing)

```java
public record OAuthResult(
    String accessToken,
    String refreshToken,
    String teamId,
    String teamName,
    String scope,
    String botUserId
) {}
```

### OAuthException (Existing)

```java
public static class OAuthException extends Exception {
    public OAuthException(String message);
    public OAuthException(String message, Throwable cause);
}
```

---

## Architecture Alignment

### Follows ARCHITECTURE.md Specifications

**System Tray Integration**: ✓ Specified in Core Service component
- Windows auto-start registration
- System tray integration
- Resource monitoring and throttling

**Token Refresh**: ✓ Specified in Slack Integration Layer
- OAuth flow handling
- Rate limiting and retry logic
- Automatic token refresh before expiration

**Silent Operation**: ✓ Follows error handling strategy
- Errors logged to file only
- No popup dialogs or notifications (except critical)
- Graceful degradation

### No Architecture Deviations

All implementations follow the architecture specifications exactly. No deviations required.

---

## Dependencies

### Required Services

**SystemTrayManager**:
- ErrorHandler (error logging)
- AutoStartManager (menu toggle)
- StatusWindow (status display)

**StatusWindow**:
- ErrorHandler (error logging)
- DatabaseManager (connection status)

**AutoStartManager**:
- ErrorHandler (error logging)
- JNA (Windows Registry access)

**OAuthManager (Token Refresh)**:
- CredentialManager (token storage)
- ErrorHandler (error logging)
- Slack SDK (OAuth API)

**SlackApiClient (Auto Refresh)**:
- OAuthManager (token refresh)
- CredentialManager (token storage)
- ErrorHandler (error logging)

### External Dependencies

All dependencies already added in Epic 1:
- JNA 5.14.0 (Windows integration)
- Slack SDK 1.45.4 (OAuth API)
- JavaFX 21.0.2 (System tray - but using AWT instead for better Windows support)
- Java AWT (built-in, used for system tray)

---

## Testing

### Unit Tests

**AutoStartManagerTest**:
- Test enable/disable auto-start
- Test registry value creation
- Test status checking
- Test verification

**OAuthManagerTokenRefreshTest**:
- Test token expiration detection
- Test refresh token flow
- Test error handling
- Test credential management

### Integration Testing

**System Tray**:
- Manual testing required (visual UI)
- Verify menu items appear
- Verify menu actions work
- Verify graceful shutdown

**Auto-Start**:
- Manual testing required
- Add registry entry
- Restart Windows
- Verify application starts
- Remove registry entry

**Token Refresh**:
- Mock Slack API to return 401
- Verify automatic retry
- Verify token update in credential manager

---

## Security Considerations

**Registry Access**:
- User-level registry only (HKEY_CURRENT_USER)
- No admin privileges required
- Protected by Windows user account security

**Token Storage**:
- Tokens stored in Windows Registry with Base64 encoding
- User-specific storage (HKEY_CURRENT_USER)
- Protected by Windows ACLs

**Token Refresh**:
- Refresh token never exposed to user
- Automatic refresh transparent to user
- Failed refresh requires re-authorization

---

## Performance Impact

**System Tray**: Negligible (< 1MB memory, event-driven)

**Auto-Start**: One-time registry write (< 1ms)

**Token Refresh**: Network call to Slack API (< 500ms) only when token expires

**Overall**: No measurable performance impact on application operation

---

## Future Enhancements (Post-Epic 2)

**System Tray**:
- Add configuration submenu (hidden by default)
- Show real-time sync progress
- Display neural network learning status

**Auto-Start**:
- Add delay option (start X seconds after login)
- Add minimized start option
- Persist user preference in database

**Token Refresh**:
- Proactive refresh before expiration
- Background refresh thread
- Refresh status in status window

---

## Contact Points for Integration

**Frontend (if any)**: System tray is the primary UI, no separate frontend needed

**Backend Services**:
- MessageCollector should use SlackApiClient.executeWithTokenRefresh()
- Any service using Slack API should wrap calls with token refresh

**Database**: StatusWindow queries system_state table for last sync time

**Neural Network**: Future integration for showing learning status in status window

---

## Summary

This interface document provides complete integration specifications for Epic 2 infrastructure features. All components are designed to work seamlessly with existing Epic 1 infrastructure, requiring minimal changes to existing code. The token refresh mechanism is transparent to all Slack API consumers through the SlackApiClient wrapper.
