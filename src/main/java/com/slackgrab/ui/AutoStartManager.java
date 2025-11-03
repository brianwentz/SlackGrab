package com.slackgrab.ui;

import com.google.inject.Inject;
import com.slackgrab.core.ErrorHandler;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Windows auto-start manager
 *
 * Manages registration of SlackGrab in Windows Registry for automatic startup on login.
 * Uses Windows Run registry key: HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run
 *
 * Provides:
 * - Enable auto-start (add registry entry)
 * - Disable auto-start (remove registry entry)
 * - Check auto-start status
 *
 * Registry value contains path to application JAR or executable.
 */
public class AutoStartManager {
    private static final Logger logger = LoggerFactory.getLogger(AutoStartManager.class);

    // Windows Registry key for auto-start applications
    private static final String RUN_KEY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "SlackGrab";

    private final ErrorHandler errorHandler;

    @Inject
    public AutoStartManager(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Enable auto-start on Windows login
     *
     * Creates registry entry with path to application executable/JAR.
     *
     * @return true if auto-start enabled successfully
     */
    public boolean enableAutoStart() {
        try {
            logger.info("Enabling auto-start...");

            String applicationPath = getApplicationPath();
            if (applicationPath == null) {
                logger.error("Failed to determine application path");
                return false;
            }

            // Create command to launch application
            String launchCommand = buildLaunchCommand(applicationPath);

            logger.debug("Setting auto-start registry entry: {}", launchCommand);

            // Write to registry
            Advapi32Util.registrySetStringValue(
                WinReg.HKEY_CURRENT_USER,
                RUN_KEY_PATH,
                APP_NAME,
                launchCommand
            );

            logger.info("Auto-start enabled successfully");
            return true;

        } catch (Exception e) {
            errorHandler.handleError("Failed to enable auto-start", e);
            return false;
        }
    }

    /**
     * Disable auto-start on Windows login
     *
     * Removes registry entry if it exists.
     *
     * @return true if auto-start disabled successfully
     */
    public boolean disableAutoStart() {
        try {
            logger.info("Disabling auto-start...");

            // Check if registry value exists
            if (!isAutoStartEnabled()) {
                logger.debug("Auto-start already disabled");
                return true;
            }

            // Delete registry value
            Advapi32Util.registryDeleteValue(
                WinReg.HKEY_CURRENT_USER,
                RUN_KEY_PATH,
                APP_NAME
            );

            logger.info("Auto-start disabled successfully");
            return true;

        } catch (Exception e) {
            errorHandler.handleError("Failed to disable auto-start", e);
            return false;
        }
    }

    /**
     * Check if auto-start is currently enabled
     *
     * @return true if registry entry exists
     */
    public boolean isAutoStartEnabled() {
        try {
            // Check if registry key exists
            if (!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, RUN_KEY_PATH)) {
                logger.debug("Registry key does not exist: {}", RUN_KEY_PATH);
                return false;
            }

            // Check if our value exists
            boolean exists = Advapi32Util.registryValueExists(
                WinReg.HKEY_CURRENT_USER,
                RUN_KEY_PATH,
                APP_NAME
            );

            logger.debug("Auto-start enabled: {}", exists);
            return exists;

        } catch (Exception e) {
            logger.warn("Failed to check auto-start status", e);
            return false;
        }
    }

    /**
     * Get current auto-start registry value
     *
     * @return Registry value if exists, empty string otherwise
     */
    public String getAutoStartCommand() {
        try {
            if (!isAutoStartEnabled()) {
                return "";
            }

            return Advapi32Util.registryGetStringValue(
                WinReg.HKEY_CURRENT_USER,
                RUN_KEY_PATH,
                APP_NAME
            );

        } catch (Exception e) {
            logger.warn("Failed to get auto-start command", e);
            return "";
        }
    }

    /**
     * Get path to application executable or JAR
     *
     * Handles both development mode (running from IDE/Gradle) and production mode (installed).
     *
     * @return Path to application, or null if cannot be determined
     */
    private String getApplicationPath() {
        try {
            // Get path to current JAR or class file
            String jarPath = AutoStartManager.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath();

            // Remove leading slash on Windows (e.g., "/C:/..." -> "C:/...")
            if (jarPath.startsWith("/") && jarPath.contains(":")) {
                jarPath = jarPath.substring(1);
            }

            // Convert to proper Windows path format
            jarPath = jarPath.replace("/", "\\");

            logger.debug("Detected application path: {}", jarPath);

            // Check if path exists
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                logger.warn("Application path does not exist: {}", jarPath);
                return null;
            }

            return jarPath;

        } catch (Exception e) {
            logger.error("Failed to determine application path", e);
            return null;
        }
    }

    /**
     * Build launch command for auto-start
     *
     * Creates proper command line to launch the application:
     * - For JAR files: java -jar "path\to\app.jar"
     * - For EXE files: "path\to\app.exe"
     *
     * @param applicationPath Path to application
     * @return Launch command string
     */
    private String buildLaunchCommand(String applicationPath) {
        // Determine if this is a JAR or EXE
        if (applicationPath.toLowerCase().endsWith(".jar")) {
            // JAR file - need to launch with Java
            String javaPath = System.getProperty("java.home") + "\\bin\\javaw.exe";
            return String.format("\"%s\" -jar \"%s\"", javaPath, applicationPath);
        } else if (applicationPath.toLowerCase().endsWith(".exe")) {
            // Executable - launch directly
            return String.format("\"%s\"", applicationPath);
        } else {
            // Unknown format - try to launch with Java
            logger.warn("Unknown application format, attempting Java launch: {}", applicationPath);
            String javaPath = System.getProperty("java.home") + "\\bin\\javaw.exe";
            return String.format("\"%s\" -jar \"%s\"", javaPath, applicationPath);
        }
    }

    /**
     * Verify auto-start configuration
     *
     * Checks if auto-start is properly configured and path is valid.
     *
     * @return true if auto-start is configured and valid
     */
    public boolean verifyAutoStart() {
        if (!isAutoStartEnabled()) {
            return false;
        }

        try {
            String command = getAutoStartCommand();
            logger.debug("Verifying auto-start command: {}", command);

            // Basic validation - command should not be empty
            return !command.isEmpty();

        } catch (Exception e) {
            logger.warn("Failed to verify auto-start", e);
            return false;
        }
    }
}
