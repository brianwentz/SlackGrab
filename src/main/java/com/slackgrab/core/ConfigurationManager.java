package com.slackgrab.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Internal configuration management
 *
 * Note: This is NOT user-facing configuration. No settings are exposed to users.
 * All configuration is for internal system operation only (zero configuration principle).
 *
 * Settings determined by:
 * - Moderate defaults that work for everyone
 * - Behavioral learning and adaptation
 * - System resource detection
 */
public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    // Application directories
    private final Path appDataPath;
    private final Path logsPath;
    private final Path databasePath;
    private final Path cachePath;
    private final Path modelsPath;

    // Webhook server configuration
    private final int webhookPort = 7395;
    private final String webhookHost = "localhost";

    // Resource limits
    private final long maxMemoryMB = 4096; // 4GB max
    private final double maxCpuPercent = 5.0; // 5% average max
    private final double maxGpuMemoryPercent = 80.0; // 80% GPU RAM max
    private final double cpuThrottleThreshold = 80.0; // Pause training at 80% CPU

    // Message processing limits
    private final int maxMessagesPerDay = 5000;
    private final int maxChannels = 2000;
    private final int historicalDataDays = 30;

    // Performance targets
    private final long scoringLatencyMs = 1000; // < 1 second
    private final long apiResponseMs = 100; // < 100ms

    public ConfigurationManager() {
        logger.info("Initializing configuration manager...");

        // Initialize application directories
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null) {
            throw new IllegalStateException("LOCALAPPDATA environment variable not set - requires Windows 11+");
        }

        this.appDataPath = Paths.get(localAppData, "SlackGrab");
        this.logsPath = appDataPath.resolve("logs");
        this.databasePath = appDataPath.resolve("database");
        this.cachePath = appDataPath.resolve("cache");
        this.modelsPath = appDataPath.resolve("models");

        createDirectories();

        logger.info("Configuration initialized. App data path: {}", appDataPath);
    }

    private void createDirectories() {
        try {
            Files.createDirectories(appDataPath);
            Files.createDirectories(logsPath);
            Files.createDirectories(databasePath);
            Files.createDirectories(cachePath);
            Files.createDirectories(modelsPath);

            logger.info("Application directories created successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create application directories", e);
        }
    }

    // Getters for paths
    public Path getAppDataPath() { return appDataPath; }
    public Path getLogsPath() { return logsPath; }
    public Path getDatabasePath() { return databasePath; }
    public Path getCachePath() { return cachePath; }
    public Path getModelsPath() { return modelsPath; }

    // Webhook configuration
    public int getWebhookPort() { return webhookPort; }
    public String getWebhookHost() { return webhookHost; }

    // Resource limits
    public long getMaxMemoryMB() { return maxMemoryMB; }
    public double getMaxCpuPercent() { return maxCpuPercent; }
    public double getMaxGpuMemoryPercent() { return maxGpuMemoryPercent; }
    public double getCpuThrottleThreshold() { return cpuThrottleThreshold; }

    // Message processing limits
    public int getMaxMessagesPerDay() { return maxMessagesPerDay; }
    public int getMaxChannels() { return maxChannels; }
    public int getHistoricalDataDays() { return historicalDataDays; }

    // Performance targets
    public long getScoringLatencyMs() { return scoringLatencyMs; }
    public long getApiResponseMs() { return apiResponseMs; }
}
