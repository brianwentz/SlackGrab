# Slack App Setup Guide for SlackGrab

This guide walks you through creating and configuring a Slack App to use with SlackGrab.

---

## Prerequisites

- A Slack workspace where you have permission to install apps
- Admin access (or ability to request app installation)
- SlackGrab application installed on your Windows machine

---

## Step 1: Create a Slack App

### 1.1 Go to Slack API Dashboard

1. Open your browser and navigate to: https://api.slack.com/apps
2. Click the **"Create New App"** button
3. Select **"From scratch"**

### 1.2 Configure Basic Information

1. **App Name:** `SlackGrab` (or your preferred name)
2. **Development Slack Workspace:** Select your workspace from the dropdown
3. Click **"Create App"**

You'll be redirected to your app's configuration page.

---

## Step 2: Configure OAuth & Permissions

### 2.1 Set Up Redirect URL

1. In the left sidebar, click **"OAuth & Permissions"**
2. Scroll down to **"Redirect URLs"** section
3. Click **"Add New Redirect URL"**
4. Enter: `http://localhost:7395/slack/oauth/callback`
5. Click **"Add"**
6. Click **"Save URLs"**

**Important:** This URL must match exactly. SlackGrab's webhook server runs on port 7395.

### 2.2 Configure Bot Token Scopes

Scroll down to the **"Scopes"** section and click **"Add an OAuth Scope"** under **"Bot Token Scopes"**.

Add the following scopes:

#### Required Scopes:
- **`channels:history`** - View messages in public channels
- **`channels:read`** - View basic channel information
- **`users:read`** - View people in the workspace

#### Optional (Recommended) Scopes:
- **`groups:history`** - View messages in private channels (user is member of)
- **`groups:read`** - View basic private channel information
- **`im:history`** - View messages in direct messages
- **`im:read`** - View basic direct message information
- **`mpim:history`** - View messages in group direct messages
- **`mpim:read`** - View basic group DM information
- **`team:read`** - View workspace name and details

**Note:** The more scopes you add, the more messages SlackGrab can analyze. Start with the required scopes and add optional ones as needed.

---

## Step 3: Get Your Credentials

### 3.1 Copy Client ID and Client Secret

1. In the left sidebar, click **"Basic Information"**
2. Scroll down to **"App Credentials"** section
3. You'll see:
   - **Client ID** (e.g., `1234567890.1234567890123`)
   - **Client Secret** (click "Show" to reveal)
4. **Copy both values** - you'll need them in the next step

**Security Note:** Keep your Client Secret private. Never commit it to version control.

---

## Step 4: Configure SlackGrab with Your Credentials

### 4.1 Set Environment Variables (Windows)

You have two options:

#### Option A: System Environment Variables (Persistent)

1. Open **Start Menu** → Search for "Environment Variables"
2. Click **"Edit the system environment variables"**
3. Click **"Environment Variables"** button
4. Under **"User variables"**, click **"New"**
5. Add the following variables:

**Variable 1:**
- Name: `SLACK_CLIENT_ID`
- Value: `[paste your Client ID]`

**Variable 2:**
- Name: `SLACK_CLIENT_SECRET`
- Value: `[paste your Client Secret]`

6. Click **"OK"** on all dialogs
7. **Restart any open command prompts** or IDEs for changes to take effect

#### Option B: Command Line (Temporary - Current Session Only)

Open PowerShell or Command Prompt and run:

```powershell
# PowerShell
$env:SLACK_CLIENT_ID="your-client-id-here"
$env:SLACK_CLIENT_SECRET="your-client-secret-here"

# Then run SlackGrab from the same terminal
./gradlew run
```

Or for Command Prompt:
```cmd
set SLACK_CLIENT_ID=your-client-id-here
set SLACK_CLIENT_SECRET=your-client-secret-here
gradlew.bat run
```

### 4.2 Verify Environment Variables

Open a new terminal and verify:

```powershell
# PowerShell
echo $env:SLACK_CLIENT_ID
echo $env:SLACK_CLIENT_SECRET

# Command Prompt
echo %SLACK_CLIENT_ID%
echo %SLACK_CLIENT_SECRET%
```

Both should display your values (not empty).

---

## Step 5: Install the App to Your Workspace

### 5.1 Install to Development Workspace

1. In the Slack API dashboard, go to **"Install App"** (left sidebar)
2. Click **"Install to Workspace"**
3. Review the permissions
4. Click **"Allow"**

**Note:** You can skip this step if you plan to use the OAuth flow from SlackGrab directly (recommended).

---

## Step 6: Run SlackGrab

### 6.1 Start the Application

```bash
cd C:\Users\brian\source\repos\SlackGrab
./gradlew run
```

Or run the built executable if you've created one.

### 6.2 Connect to Slack

1. After SlackGrab starts, you'll see a **system tray icon** (notification area, bottom-right of Windows taskbar)
2. The icon tooltip will show: **"SlackGrab - Not Connected"**
3. You'll see a notification: **"Welcome to SlackGrab. Right-click the tray icon and select 'Connect to Slack' to get started."**

### 6.3 Authorize SlackGrab

1. **Right-click** the SlackGrab tray icon
2. Select **"Connect to Slack"**
3. Your default browser will open to the Slack authorization page
4. Review the permissions and click **"Allow"**
5. You'll be redirected to a success page showing: **"Authorization Successful! Connected to [Your Workspace Name]"**
6. The tray icon tooltip will update to: **"SlackGrab - Connected"**

### 6.4 Verify Connection

1. Right-click the tray icon
2. Select **"Show Status"**
3. A window will appear showing:
   - **Connection Status:** Connected to Slack ✓ (green)
   - **Workspace:** [Your Team ID]
   - **Last Sync:** [timestamp]
   - **Messages Collected:** [count]
   - **Current State:** Running (green)

---

## Step 7: Verify Message Collection

### 7.1 Check Database

Messages are stored in:
```
C:\Users\[YourUsername]\AppData\Local\SlackGrab\database\slackgrab.db
```

You can open this with any SQLite browser to see collected messages.

### 7.2 Check Logs

Logs are stored in:
```
C:\Users\[YourUsername]\AppData\Local\SlackGrab\logs\slackgrab.log
```

Look for entries like:
- "Successfully authenticated with Slack"
- "Starting message collection"
- "Collected X messages from channel Y"

---

## Troubleshooting

### Issue: "SLACK_CLIENT_ID environment variable not set"

**Solution:** Environment variables not configured correctly.
- Verify you set both `SLACK_CLIENT_ID` and `SLACK_CLIENT_SECRET`
- Restart your terminal/IDE after setting environment variables
- Use system environment variables (Option A above) for persistence

### Issue: "Redirect URI mismatch"

**Solution:** The redirect URL in your Slack App doesn't match.
- Go to Slack API dashboard → OAuth & Permissions
- Verify redirect URL is exactly: `http://localhost:7395/slack/oauth/callback`
- No trailing slashes, exact port number

### Issue: "Failed to open browser for Slack authorization"

**Solution:**
- Manually open the browser and navigate to the URL shown in the error
- Check if another application is using port 7395
- Verify WebhookServer started successfully (check logs)

### Issue: "Token expired" or "Invalid auth"

**Solution:** Re-authorize the application.
- Right-click tray icon → Exit
- Clear stored credentials from Windows Registry:
  - Open Registry Editor (regedit)
  - Navigate to: `HKEY_CURRENT_USER\Software\SlackGrab\Credentials`
  - Delete the key
- Restart SlackGrab
- Connect to Slack again

### Issue: No messages being collected

**Solution:**
- Verify you granted the required scopes (channels:history, channels:read)
- Check that SlackGrab has access to the channels you want to monitor
- Look in logs for API errors or rate limiting messages
- Verify the workspace has messages in channels (last 30 days)

---

## Security Best Practices

### 1. Protect Your Credentials

- **Never commit** `SLACK_CLIENT_ID` or `SLACK_CLIENT_SECRET` to version control
- **Never share** your Client Secret publicly
- Use environment variables or secure credential storage
- Rotate credentials if compromised

### 2. Scope Permissions Appropriately

- Start with minimal scopes (channels:history, channels:read, users:read)
- Add additional scopes only as needed
- Review what data each scope grants access to
- Users will see all requested permissions during OAuth

### 3. Secure Your Machine

- SlackGrab stores tokens in Windows Registry (user-specific)
- Tokens are Base64-encoded (not encrypted in current version)
- Ensure your Windows user account is password-protected
- Consider enabling BitLocker for full disk encryption

### 4. Monitor Access

- Periodically review installed apps in Slack workspace
- Check SlackGrab logs for unusual activity
- Revoke access if you suspect compromise:
  - Go to Slack workspace settings → Apps → SlackGrab → Remove

---

## Uninstalling

### Remove SlackGrab from Slack

1. Go to your Slack workspace
2. Click workspace name → **Settings & administration** → **Manage apps**
3. Find **SlackGrab** → Click → **Remove App**

### Remove SlackGrab from Windows

1. Exit SlackGrab (right-click tray icon → Exit)
2. Delete application folder
3. Delete data folder: `C:\Users\[You]\AppData\Local\SlackGrab`
4. Remove environment variables (if set as system variables)
5. Remove Registry entries:
   - `HKEY_CURRENT_USER\Software\SlackGrab`
   - `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run\SlackGrab` (if auto-start enabled)

---

## Advanced Configuration

### Custom Port

If port 7395 is already in use, you can change it:

1. Update `ConfigurationManager.java`:
   ```java
   private static final int WEBHOOK_PORT = 7395; // Change this
   ```
2. Rebuild the application
3. Update the Redirect URL in Slack App settings to match

### Custom Data Directory

By default, data is stored in `%LOCALAPPDATA%\SlackGrab`. To change:

1. Update `ConfigurationManager.java`:
   ```java
   private final Path appDataPath = Paths.get(
       System.getenv("LOCALAPPDATA"), "SlackGrab" // Change "SlackGrab" to your preferred folder
   );
   ```
2. Rebuild the application

### Development Mode

For development/testing with multiple Slack workspaces:

1. Create separate Slack Apps for each workspace
2. Use different environment variable names:
   ```
   SLACK_CLIENT_ID_DEV
   SLACK_CLIENT_ID_PROD
   ```
3. Modify code to read the appropriate set based on configuration

---

## Support

### Slack API Documentation
- OAuth Guide: https://api.slack.com/authentication/oauth-v2
- Scopes Reference: https://api.slack.com/scopes
- Web API Methods: https://api.slack.com/methods

### SlackGrab Issues
- GitHub Issues: https://github.com/brianwentz/SlackGrab/issues
- Check logs: `%LOCALAPPDATA%\SlackGrab\logs\slackgrab.log`

---

## Quick Reference

### Required Environment Variables
```
SLACK_CLIENT_ID=your-client-id
SLACK_CLIENT_SECRET=your-client-secret
```

### OAuth Redirect URL
```
http://localhost:7395/slack/oauth/callback
```

### Minimum Required Scopes
```
channels:history
channels:read
users:read
```

### Data Locations
- **Database:** `%LOCALAPPDATA%\SlackGrab\database\slackgrab.db`
- **Logs:** `%LOCALAPPDATA%\SlackGrab\logs\slackgrab.log`
- **Credentials:** Windows Registry `HKCU\Software\SlackGrab\Credentials`

---

**You're all set!** SlackGrab should now be collecting and analyzing your Slack messages.
