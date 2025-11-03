package com.slackgrab.ui;

import com.google.inject.Inject;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.core.ManagedService;
import com.slackgrab.core.ServiceCoordinator;
import com.slackgrab.oauth.OAuthManager;
import com.slackgrab.security.CredentialManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.imageio.ImageIO;

/**
 * Windows system tray integration manager
 *
 * Provides system tray icon with right-click context menu for:
 * - Connecting to Slack workspace (OAuth flow)
 * - Viewing application status
 * - Accessing settings (minimal, zero-config principle)
 * - Graceful application exit
 *
 * Implements silent background operation with tray-based interaction.
 */
public class SystemTrayManager implements ManagedService {
    private static final Logger logger = LoggerFactory.getLogger(SystemTrayManager.class);

    private final ErrorHandler errorHandler;
    private final AutoStartManager autoStartManager;
    private final StatusWindow statusWindow;
    private final ServiceCoordinator serviceCoordinator;
    private final OAuthManager oauthManager;
    private final CredentialManager credentialManager;

    private TrayIcon trayIcon;
    private SystemTray systemTray;
    private boolean started = false;

    @Inject
    public SystemTrayManager(
            ErrorHandler errorHandler,
            AutoStartManager autoStartManager,
            StatusWindow statusWindow,
            ServiceCoordinator serviceCoordinator,
            OAuthManager oauthManager,
            CredentialManager credentialManager) {
        this.errorHandler = errorHandler;
        this.autoStartManager = autoStartManager;
        this.statusWindow = statusWindow;
        this.serviceCoordinator = serviceCoordinator;
        this.oauthManager = oauthManager;
        this.credentialManager = credentialManager;
    }

    /**
     * Initialize and display system tray icon
     */
    @Override
    public void start() throws Exception {
        if (started) {
            logger.warn("SystemTrayManager already started");
            return;
        }

        if (!SystemTray.isSupported()) {
            logger.error("System tray not supported on this platform");
            throw new UnsupportedOperationException("System tray not supported");
        }

        try {
            logger.info("Initializing system tray icon...");

            // Get system tray instance
            systemTray = SystemTray.getSystemTray();

            // Load tray icon image
            Image image = loadTrayIcon();

            // Create popup menu
            PopupMenu popup = createPopupMenu();

            // Create tray icon with tooltip and menu
            trayIcon = new TrayIcon(image, "SlackGrab - Message Prioritization", popup);
            trayIcon.setImageAutoSize(true);

            // Add double-click listener to show status
            trayIcon.addActionListener(e -> showStatus());

            // Add to system tray
            systemTray.add(trayIcon);

            started = true;
            logger.info("System tray icon initialized successfully");

            // Check connection status and notify user
            checkConnectionStatus();

        } catch (Exception e) {
            errorHandler.handleError("Failed to initialize system tray", e);
            throw e;
        }
    }

    /**
     * Remove system tray icon
     */
    @Override
    public void stop() throws Exception {
        if (!started) {
            logger.warn("SystemTrayManager not started, nothing to stop");
            return;
        }

        try {
            logger.info("Removing system tray icon...");

            if (systemTray != null && trayIcon != null) {
                systemTray.remove(trayIcon);
            }

            // Close status window if open
            if (statusWindow != null) {
                statusWindow.hide();
            }

            started = false;
            logger.info("System tray icon removed successfully");

        } catch (Exception e) {
            errorHandler.handleError("Error removing system tray icon", e);
            throw e;
        }
    }

    /**
     * Display system tray notification
     *
     * @param title Notification title
     * @param message Notification message
     * @param messageType Type of notification
     */
    public void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, messageType);
                logger.debug("Displayed notification: {}", title);
            } catch (Exception e) {
                errorHandler.handleError("Failed to display notification", e);
            }
        }
    }

    /**
     * Show error notification (only for critical errors per architecture)
     *
     * @param message Error message
     */
    public void showErrorNotification(String message) {
        showNotification("SlackGrab Error", message, TrayIcon.MessageType.ERROR);
    }

    /**
     * Show info notification
     *
     * @param message Info message
     */
    public void showInfoNotification(String message) {
        showNotification("SlackGrab", message, TrayIcon.MessageType.INFO);
    }

    /**
     * Create popup menu with all menu items
     */
    private PopupMenu createPopupMenu() {
        PopupMenu popup = new PopupMenu();

        // Connect to Slack (only show if not connected)
        if (!credentialManager.hasAccessToken()) {
            MenuItem connectItem = new MenuItem("Connect to Slack");
            connectItem.addActionListener(e -> initiateOAuthFlow());
            popup.add(connectItem);
            popup.addSeparator();
        }

        // Show Status
        MenuItem statusItem = new MenuItem("Show Status");
        statusItem.addActionListener(e -> showStatus());
        popup.add(statusItem);

        popup.addSeparator();

        // Auto-Start toggle
        CheckboxMenuItem autoStartItem = new CheckboxMenuItem("Start on Windows Login");
        autoStartItem.setState(autoStartManager.isAutoStartEnabled());
        autoStartItem.addItemListener(e -> toggleAutoStart(autoStartItem));
        popup.add(autoStartItem);

        popup.addSeparator();

        // Exit
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> exitApplication());
        popup.add(exitItem);

        return popup;
    }

    /**
     * Show status window with current application state
     */
    private void showStatus() {
        try {
            logger.debug("Showing status window");
            statusWindow.show();
        } catch (Exception e) {
            errorHandler.handleError("Failed to show status window", e);
            showErrorNotification("Failed to open status window");
        }
    }

    /**
     * Toggle auto-start setting
     */
    private void toggleAutoStart(CheckboxMenuItem menuItem) {
        try {
            boolean enabled = menuItem.getState();
            logger.info("Toggling auto-start: {}", enabled);

            if (enabled) {
                autoStartManager.enableAutoStart();
                showInfoNotification("SlackGrab will start automatically on Windows login");
            } else {
                autoStartManager.disableAutoStart();
                showInfoNotification("Auto-start disabled");
            }

        } catch (Exception e) {
            errorHandler.handleError("Failed to toggle auto-start", e);
            // Revert checkbox state
            menuItem.setState(!menuItem.getState());
            showErrorNotification("Failed to update auto-start setting");
        }
    }

    /**
     * Exit application gracefully
     */
    private void exitApplication() {
        logger.info("Exit requested from system tray");

        try {
            // Remove tray icon first
            if (systemTray != null && trayIcon != null) {
                systemTray.remove(trayIcon);
                logger.debug("Tray icon removed");
            }

            // Shutdown all services gracefully
            logger.info("Initiating graceful shutdown of all services...");
            serviceCoordinator.shutdown();
            logger.info("All services shut down successfully");

            // Exit JVM
            // The shutdown hook in SlackGrabApplication will be called, but
            // services are already stopped, so it will be a no-op
            System.exit(0);

        } catch (Exception e) {
            errorHandler.handleError("Error during application exit", e);
            // Force exit if graceful shutdown fails
            Runtime.getRuntime().halt(1);
        }
    }

    /**
     * Load tray icon image from resources
     */
    private Image loadTrayIcon() throws IOException {
        // Try to load custom icon from resources
        try (InputStream iconStream = getClass().getResourceAsStream("/tray-icon.png")) {
            if (iconStream != null) {
                logger.debug("Loading custom tray icon");
                return ImageIO.read(iconStream);
            }
        } catch (Exception e) {
            logger.warn("Failed to load custom tray icon, using default", e);
        }

        // Fallback: create simple default icon (16x16 blue square)
        logger.debug("Using default tray icon");
        return createDefaultIcon();
    }

    /**
     * Create simple default tray icon
     */
    private Image createDefaultIcon() {
        // Create 16x16 image with SlackGrab branding
        int size = 16;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw blue circle with white "S"
        g2d.setColor(new Color(70, 130, 180)); // Steel blue
        g2d.fillOval(0, 0, size, size);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("S", 4, 13);

        g2d.dispose();

        return image;
    }

    /**
     * Check if system tray is running
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Check connection status on startup and notify user
     */
    private void checkConnectionStatus() {
        try {
            if (credentialManager.hasAccessToken()) {
                // Already authenticated
                logger.info("Already connected to Slack");
                updateTrayTooltip("SlackGrab - Connected");

                // Show subtle notification
                showInfoNotification("Connected to Slack workspace");
            } else {
                // Need to connect
                logger.info("Not connected to Slack - user needs to authorize");
                updateTrayTooltip("SlackGrab - Not Connected");

                // Show notification prompting user to connect
                trayIcon.displayMessage(
                    "Welcome to SlackGrab",
                    "Right-click the tray icon and select 'Connect to Slack' to get started.",
                    TrayIcon.MessageType.INFO
                );
            }
        } catch (Exception e) {
            errorHandler.handleError("Failed to check connection status", e);
        }
    }

    /**
     * Update tray icon tooltip
     */
    private void updateTrayTooltip(String tooltip) {
        if (trayIcon != null) {
            trayIcon.setToolTip(tooltip);
        }
    }

    /**
     * Initiate OAuth flow by opening browser to Slack authorization URL
     */
    private void initiateOAuthFlow() {
        try {
            logger.info("Initiating OAuth flow...");

            // Get OAuth URL from OAuthManager
            String authUrl = oauthManager.generateAuthorizationUrl();
            logger.debug("Generated OAuth URL: {}", authUrl);

            // Open in default browser
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));

                // Show notification
                trayIcon.displayMessage(
                    "Slack Authorization",
                    "Browser opened for Slack authorization. Please authorize SlackGrab to access your workspace.",
                    TrayIcon.MessageType.INFO
                );

                logger.info("OAuth flow initiated successfully");
            } else {
                logger.error("Desktop browsing not supported");
                trayIcon.displayMessage(
                    "Browser Error",
                    "Unable to open browser automatically. Please visit the OAuth URL manually.",
                    TrayIcon.MessageType.ERROR
                );
            }

        } catch (IllegalStateException e) {
            // OAuth credentials not configured
            logger.error("OAuth credentials not configured", e);
            errorHandler.handleError("OAuth configuration error", e);
            trayIcon.displayMessage(
                "Configuration Error",
                "Slack OAuth credentials not configured. Please set SLACK_CLIENT_ID and SLACK_CLIENT_SECRET environment variables.",
                TrayIcon.MessageType.ERROR
            );
        } catch (Exception e) {
            logger.error("Failed to initiate OAuth flow", e);
            errorHandler.handleError("Failed to initiate OAuth", e);
            trayIcon.displayMessage(
                "Connection Error",
                "Failed to open browser for Slack authorization. Please try again.",
                TrayIcon.MessageType.ERROR
            );
        }
    }

    /**
     * Update connection status after successful OAuth
     * This can be called by other components when OAuth completes
     */
    public void onOAuthSuccess() {
        try {
            logger.info("OAuth successful, updating tray status");
            updateTrayTooltip("SlackGrab - Connected");

            // Recreate menu to remove "Connect to Slack" option
            if (trayIcon != null) {
                PopupMenu newMenu = createPopupMenu();
                trayIcon.setPopupMenu(newMenu);
            }

            showInfoNotification("Successfully connected to Slack workspace!");
        } catch (Exception e) {
            errorHandler.handleError("Failed to update tray after OAuth success", e);
        }
    }
}
