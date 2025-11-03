package com.slackgrab.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ConfigurationManager
 */
class ConfigurationManagerTest {

    @Test
    void testConfigurationManagerInitialization() {
        ConfigurationManager configManager = new ConfigurationManager();

        assertThat(configManager).isNotNull();
        assertThat(configManager.getAppDataPath()).isNotNull();
        assertThat(configManager.getLogsPath()).isNotNull();
        assertThat(configManager.getDatabasePath()).isNotNull();
        assertThat(configManager.getCachePath()).isNotNull();
        assertThat(configManager.getModelsPath()).isNotNull();
    }

    @Test
    void testWebhookConfiguration() {
        ConfigurationManager configManager = new ConfigurationManager();

        assertThat(configManager.getWebhookPort()).isEqualTo(7395);
        assertThat(configManager.getWebhookHost()).isEqualTo("localhost");
    }

    @Test
    void testResourceLimits() {
        ConfigurationManager configManager = new ConfigurationManager();

        assertThat(configManager.getMaxMemoryMB()).isEqualTo(4096);
        assertThat(configManager.getMaxCpuPercent()).isEqualTo(5.0);
        assertThat(configManager.getMaxGpuMemoryPercent()).isEqualTo(80.0);
        assertThat(configManager.getCpuThrottleThreshold()).isEqualTo(80.0);
    }

    @Test
    void testMessageProcessingLimits() {
        ConfigurationManager configManager = new ConfigurationManager();

        assertThat(configManager.getMaxMessagesPerDay()).isEqualTo(5000);
        assertThat(configManager.getMaxChannels()).isEqualTo(2000);
        assertThat(configManager.getHistoricalDataDays()).isEqualTo(30);
    }

    @Test
    void testPerformanceTargets() {
        ConfigurationManager configManager = new ConfigurationManager();

        assertThat(configManager.getScoringLatencyMs()).isEqualTo(1000);
        assertThat(configManager.getApiResponseMs()).isEqualTo(100);
    }

    @Test
    void testDirectoriesCreated() {
        ConfigurationManager configManager = new ConfigurationManager();

        assertThat(Files.exists(configManager.getAppDataPath())).isTrue();
        assertThat(Files.exists(configManager.getLogsPath())).isTrue();
        assertThat(Files.exists(configManager.getDatabasePath())).isTrue();
        assertThat(Files.exists(configManager.getCachePath())).isTrue();
        assertThat(Files.exists(configManager.getModelsPath())).isTrue();
    }
}
