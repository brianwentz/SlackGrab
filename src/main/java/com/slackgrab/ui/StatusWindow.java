package com.slackgrab.ui;

import com.google.inject.Inject;
import com.slackgrab.core.ErrorHandler;
import com.slackgrab.data.DatabaseManager;
import com.slackgrab.security.CredentialManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Status display window for SlackGrab
 *
 * Shows current application status including:
 * - Connection status (connected/disconnected)
 * - Workspace name (if connected)
 * - Last sync timestamp
 * - Messages collected count
 * - Current operational state (syncing, idle, error)
 *
 * Follows zero-configuration principle - displays information only, no user configuration.
 */
public class StatusWindow {
    private static final Logger logger = LoggerFactory.getLogger(StatusWindow.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ErrorHandler errorHandler;
    private final DatabaseManager databaseManager;
    private final CredentialManager credentialManager;

    private JFrame frame;
    private JLabel connectionStatusLabel;
    private JLabel workspaceLabel;
    private JLabel lastSyncLabel;
    private JLabel messagesCountLabel;
    private JLabel currentStateLabel;

    private boolean visible = false;

    @Inject
    public StatusWindow(ErrorHandler errorHandler, DatabaseManager databaseManager, CredentialManager credentialManager) {
        this.errorHandler = errorHandler;
        this.databaseManager = databaseManager;
        this.credentialManager = credentialManager;
    }

    /**
     * Show the status window
     */
    public void show() {
        if (frame == null) {
            createWindow();
        }

        // Update status information
        updateStatus();

        // Show window
        frame.setVisible(true);
        visible = true;

        logger.debug("Status window shown");
    }

    /**
     * Hide the status window
     */
    public void hide() {
        if (frame != null) {
            frame.setVisible(false);
            visible = false;
            logger.debug("Status window hidden");
        }
    }

    /**
     * Check if window is currently visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Create the status window UI
     */
    private void createWindow() {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Failed to set system look and feel", e);
        }

        // Create frame
        frame = new JFrame("SlackGrab Status");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setResizable(false);

        // Center on screen
        frame.setLocationRelativeTo(null);

        // Create content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("SlackGrab Status");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Status fields
        connectionStatusLabel = createStatusLabel("Connection Status:");
        contentPanel.add(connectionStatusLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        workspaceLabel = createStatusLabel("Workspace:");
        contentPanel.add(workspaceLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        lastSyncLabel = createStatusLabel("Last Sync:");
        contentPanel.add(lastSyncLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        messagesCountLabel = createStatusLabel("Messages Collected:");
        contentPanel.add(messagesCountLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        currentStateLabel = createStatusLabel("Current State:");
        contentPanel.add(currentStateLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        closeButton.addActionListener(e -> hide());
        contentPanel.add(closeButton);

        frame.add(contentPanel);

        logger.debug("Status window created");
    }

    /**
     * Create a status label with default styling
     */
    private JLabel createStatusLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Update status information in the window
     */
    private void updateStatus() {
        try {
            // Update connection status
            updateConnectionStatus();

            // Update workspace information
            updateWorkspaceInfo();

            // Update last sync time
            updateLastSyncTime();

            // Update messages count
            updateMessagesCount();

            // Update current state
            updateCurrentState();

            logger.debug("Status window updated");

        } catch (Exception e) {
            errorHandler.handleError("Failed to update status window", e);
        }
    }

    /**
     * Update connection status display
     */
    private void updateConnectionStatus() {
        // Check if OAuth token exists (indicates connected to Slack)
        boolean connected = false;
        try {
            connected = credentialManager != null && credentialManager.hasAccessToken();
        } catch (Exception e) {
            logger.warn("Failed to check connection status", e);
        }

        String status = connected ? "Connected to Slack" : "Not Connected";
        String color = connected ? "green" : "red";

        connectionStatusLabel.setText(String.format(
            "<html>Connection Status: <font color='%s'>%s</font></html>",
            color, status
        ));
    }

    /**
     * Update workspace information display
     */
    private void updateWorkspaceInfo() {
        try {
            // Get workspace/team ID if available
            Optional<String> teamId = credentialManager.getTeamId();

            if (teamId.isPresent()) {
                // In production, we would query Slack API for team name
                // For now, just show the team ID
                workspaceLabel.setText("Workspace: " + teamId.get());
            } else {
                workspaceLabel.setText("Workspace: Not Connected");
            }

        } catch (Exception e) {
            logger.warn("Failed to get workspace info", e);
            workspaceLabel.setText("Workspace: Unknown");
        }
    }

    /**
     * Update last sync timestamp
     */
    private void updateLastSyncTime() {
        try {
            // Query last message timestamp from database
            LocalDateTime lastSync = getLastSyncTime();

            if (lastSync != null) {
                String formattedTime = lastSync.format(TIME_FORMATTER);
                lastSyncLabel.setText("Last Sync: " + formattedTime);
            } else {
                lastSyncLabel.setText("Last Sync: Never");
            }

        } catch (Exception e) {
            logger.warn("Failed to get last sync time", e);
            lastSyncLabel.setText("Last Sync: Unknown");
        }
    }

    /**
     * Update messages count display
     */
    private void updateMessagesCount() {
        try {
            // Query message count from database
            long messageCount = getMessageCount();
            messagesCountLabel.setText("Messages Collected: " + messageCount);

        } catch (Exception e) {
            logger.warn("Failed to get message count", e);
            messagesCountLabel.setText("Messages Collected: Unknown");
        }
    }

    /**
     * Update current operational state
     */
    private void updateCurrentState() {
        // For now, show "Idle" or "Running"
        // In future, could show "Syncing", "Training", "Error", etc.
        String state = "Running";
        String color = "green";

        currentStateLabel.setText(String.format(
            "<html>Current State: <font color='%s'>%s</font></html>",
            color, state
        ));
    }

    /**
     * Get last sync time from database
     * Returns null if no messages synced yet
     */
    private LocalDateTime getLastSyncTime() {
        try {
            // Query system_state table for last sync time
            // For now, return current time as placeholder
            // TODO: Implement proper database query
            return LocalDateTime.now();

        } catch (Exception e) {
            logger.warn("Failed to query last sync time", e);
            return null;
        }
    }

    /**
     * Get total message count from database
     */
    private long getMessageCount() {
        try {
            // Query messages table for total count
            // For now, return 0 as placeholder
            // TODO: Implement proper database query
            return 0L;

        } catch (Exception e) {
            logger.warn("Failed to query message count", e);
            return 0L;
        }
    }
}
