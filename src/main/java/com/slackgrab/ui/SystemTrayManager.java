package com.slackgrab.ui;

import com.google.inject.Inject;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.core.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Windows system tray integration manager
 *
 * Provides system tray icon with right-click context menu for:
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

    private TrayIcon trayIcon;
    private SystemTray systemTray;
    private boolean started = false;

    @Inject
    public SystemTrayManager(
            ErrorHandler errorHandler,
            AutoStartManager autoStartManager,
            StatusWindow statusWindow) {
        this.errorHandler = errorHandler;
        this.autoStartManager = autoStartManager;
        this.statusWindow = statusWindow;
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
            // Perform graceful shutdown
            // The shutdown hook will handle cleanup
            System.exit(0);

        } catch (Exception e) {
            errorHandler.handleError("Error during application exit", e);
            // Force exit if graceful shutdown fails
            System.exit(1);
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
}
